package ox.softeng.gel.filereceive;

import ox.softeng.burst.domain.SeverityEnum;
import ox.softeng.burst.xml.MessageDTO;
import ox.softeng.gel.filereceive.config.Action;
import ox.softeng.gel.filereceive.config.Folder;
import ox.softeng.gel.filereceive.config.Header;
import ox.softeng.gel.filereceive.utils.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FolderMonitor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FolderMonitor.class);
    Channel channel;
    LoadingCache<Path, Path> copyCache;
    HashMap<Path, Long> fileSizes;
    HashMap<Path, FileTime> lastModified;
    Path monitorDir;
    Path moveDir;
    Long refreshTime;
    HashMap<Path, LocalDateTime> timeDiscovered;
    private Action action;
    private JAXBContext burstMessageContext;
    private String burstQueue;
    private Connection connection;
    private String contextPath;
    private String exchangeName;
    private Folder folder;


    public FolderMonitor(Connection connection, String contextPath, Folder folder, String exchangeName, String burstQueue, Long refreshTime)
            throws JAXBException, IOException {

        this.connection = connection;
        this.contextPath = contextPath;
        this.folder = folder;
        this.burstQueue = burstQueue;

        this.refreshTime = folder.getRefreshFrequency() == null ? refreshTime : folder.getRefreshFrequency().longValue();
        this.exchangeName = folder.getExchange() == null ? exchangeName : folder.getExchange();
        this.action = folder.getAction();

        fileSizes = new HashMap<>();
        lastModified = new HashMap<>();
        timeDiscovered = new HashMap<>();

        burstMessageContext = JAXBContext.newInstance(MessageDTO.class);

        monitorDir = Paths.get(contextPath, folder.getMonitorDirectory());
        moveDir = Paths.get(contextPath, folder.getMoveDirectory());

        if (!Files.exists(monitorDir)) {
            logger.warn("Creating 'Monitor' folder as does not exist: {}", monitorDir);
            Files.createDirectories(monitorDir);
        }
        if (!Files.exists(moveDir)) {
            logger.warn("Creating 'Move' folder as does not exist: {}", moveDir);
            Files.createDirectories(moveDir);
        }

        if (this.action == Action.COPY) initialiseCache();

        logger.debug("Monitor directory type: " + Files.getFileStore(monitorDir).type());
    }

    @Override
    public void run() {
        try {
            // Only create the channel once as we only need 1 and creation is expensive
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);

            while (true) {
                LocalDateTime currentTime = LocalDateTime.now();

                // First we'll go through and find any files to handle
                logger.trace("Checking for files to handle");
                Set<Path> filesToHandle = checkForFilesToHandle(currentTime);

                // Process any files to handle
                logger.trace("Processing {} files", filesToHandle.size());
                processFiles(filesToHandle, currentTime);

                // Handle recursive/sub folders
                logger.trace("Scanning {}", monitorDir);
                scanMonitorDirectory(currentTime);

                try {
                    // Sleep for a second
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    logger.warn("Sleep broken because: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            handleException("Error running folder monitor: {}", e);
        }

    }

    Set<Path> checkForFilesToHandle(LocalDateTime currentTime) throws IOException {
        // Keep a copy of the keyset so that we can modify the underlying hashset while iterating.
        Set<Path> paths = new HashSet<>(timeDiscovered.keySet());
        Set<Path> filesToHandle = new HashSet<>();

        for (Path path : paths) {
            logger.trace("Examining file: {}", path);

            // If the file no-longer exists, then remove it
            if (!Files.exists(path)) {
                logger.debug("File {} no-longer exists, so removing", path);
                fileSizes.remove(path);
                lastModified.remove(path);
                timeDiscovered.remove(path);
            } else {
                FileTime thisFileLastModified = Files.getLastModifiedTime(path);
                Long thisFileSize = Files.size(path);

                // if it's been modified, then update it
                if (!thisFileLastModified.equals(lastModified.get(path)) || !thisFileSize.equals(fileSizes.get(path))) {
                    logger.debug("File {} modified since last examined, updating records", path);
                    fileSizes.put(path, thisFileSize);
                    lastModified.put(path, thisFileLastModified);
                    timeDiscovered.put(path, currentTime);

                } else // It's the same file as we've seen before...
                {
                    // Only consider it if it has been there for a suitable duration
                    if (currentTime.isAfter(timeDiscovered.get(path).plusSeconds(refreshTime))) {
                        logger.debug("File {} hasn't been changed in last {} seconds, adding to list to process", path, refreshTime);
                        filesToHandle.add(path);
                    }
                }
            }
        }
        return filesToHandle;
    }

    boolean processFile(Path path, LocalDateTime currentTime) throws IOException, TimeoutException, JAXBException {

        // Just make sure we don't reprocess a file that's been cached
        if (action == Action.COPY) {
            try {
                copyCache.get(path);
                return false;
            } catch (Exception ignored) {}
        }

        logger.debug("Handling file " + path);
        byte[] message = Files.readAllBytes(path);

        String filename = path.getFileName().toString();

        AMQP.BasicProperties basicProperties = buildRabbitProperties(filename);

        // Send to folder queue
        logger.trace("Sending to rabbitmq queue '{}'", folder.getBindingKey());
        channel.basicPublish(exchangeName, folder.getBindingKey(), basicProperties, message);

        // Send to burst
        logger.trace("Sending success message to rabbitmq queue '{}'", burstQueue);
        sendBurstMessage(basicProperties, buildSuccessMessage(filename));

        // Log that the message and success message have gone
        logger.debug("Sent {} and success to rabbitmq queues '{}' and '{}'", path, burstQueue, folder.getBindingKey());

        // Perform move or copy
        switch (this.action) {
            case MOVE:
                moveFile(path, Utils.resolvePath(path, monitorDir, moveDir, currentTime));
                break;
            case COPY:
                copyFile(path, Utils.resolvePath(path, monitorDir, moveDir));
                break;
        }

        logger.info("Processed file: {}", path);
        return true;

    }

    void scanMonitorDirectory(LocalDateTime currentTime) {
        try {
            Files.walkFileTree(monitorDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    if (Files.isHidden(file)) return FileVisitResult.CONTINUE;

                    if (action == Action.COPY) {
                        try {
                            copyCache.get(file);
                            return FileVisitResult.CONTINUE;
                        } catch (Exception ignored) {}
                    }
                    // If we've not seen this file before
                    if (!lastModified.containsKey(file)) {
                        try {
                            FileTime thisFileLastModified = Files.getLastModifiedTime(file);
                            Long thisFileSize = Files.size(file);
                            logger.info("Registering file: {}", file);

                            fileSizes.put(file, thisFileSize);
                            lastModified.put(file, thisFileLastModified);
                            timeDiscovered.put(file, currentTime);
                        } catch (IOException e) {
                            logger.warn("Error registering file {} because {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            handleException(monitorDir, "Failed to scan directory " + monitorDir + " because {}", e);
        }
    }

    private byte[] buildErrorMessage(String filename, Throwable throwable) throws JAXBException {
        return buildMessage("Encountered an exception with filename '" + filename + "':\n" + throwable.getMessage(),
                            filename, SeverityEnum.ERROR, "File Receipt Failure", "Error");
    }

    private byte[] buildMessage(String details, String filename, SeverityEnum severity, String... topics) throws JAXBException {
        MessageDTO burstMessage = new MessageDTO();
        burstMessage.setDateTimeCreated(OffsetDateTime.now());
        burstMessage.setSeverity(severity);
        burstMessage.setSource("Folder Monitoring System");
        String GMCName = "Unknown GMC";
        if (folder.getHeaders() != null) {
            for (Header h : folder.getHeaders().getHeader()) {
                if ("GMC".equalsIgnoreCase(h.getKey())) {
                    GMCName = h.getValue();
                    break;
                }
            }
        }
        burstMessage.setDetails(GMCName + " has: " + details);
        for (String topic : topics) {
            burstMessage.addTopic(topic);
        }
        burstMessage.addMetadata(new MessageDTO.Metadata("GMC", GMCName));
        burstMessage.addMetadata(new MessageDTO.Metadata("File name", filename));

        Marshaller m = burstMessageContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        m.marshal(burstMessage, bas);
        return bas.toByteArray();
    }

    private AMQP.BasicProperties buildRabbitProperties(String filename) {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("filename", filename);
        headerMap.put("directory", monitorDir.toString());
        headerMap.put("receivedDateTime", OffsetDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_DATE_TIME));

        if (folder.getHeaders() != null) folder.getHeaders().getHeader().forEach(it -> headerMap.put(it.getKey(), it.getValue()));

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();

        // Set headers and other required properties
        builder.headers(headerMap);
        builder.appId("folder_monitor_" + contextPath);
        builder.messageId(filename + "_" + headerMap.get("receivedDateTime"));
        builder.timestamp(Date.from(OffsetDateTime.now(ZoneId.systemDefault()).toInstant()));
        if (headerMap.containsKey("type")) {
            builder.type(headerMap.get("type").toString());
        } else builder.type("file");
        builder.contentType(determineContentType(filename));

        // Make sure the message is persisted incase of failure
        builder.deliveryMode(2);

        return builder.build();
    }

    private byte[] buildSuccessMessage(String filename) throws JAXBException {
        return buildMessage("Uploaded a file with the name '" + filename + "'", filename, SeverityEnum.NOTICE, "File Receipt");
    }

    private void copyFile(Path location, Path destination) throws IOException {
        logger.debug("Copying to " + destination);
        Files.createDirectories(destination.getParent());
        Files.copy(location, destination);
        copyCache.put(location, destination);
    }

    private String determineContentType(String filename) {
        String ext = com.google.common.io.Files.getFileExtension(filename);

        switch (ext.toLowerCase()) {
            case "xml":
                return "text/xml";
            case "csv":
                return "text/csv";
            default:
                return null;
        }
    }

    private void handleException(String message, Throwable throwable) {
        handleException("no file", message, throwable);
    }

    private void handleException(Path path, String message, Throwable throwable) {
        handleException(path.toString(), message, throwable);
    }

    private void handleException(String filename, String message, Throwable throwable) {
        logger.error(message, throwable.getMessage());
        throwable.printStackTrace();
        try {
            sendBurstMessage(buildRabbitProperties(filename), buildErrorMessage(filename, throwable));
        } catch (IOException | JAXBException ignored) {}
    }

    private void initialiseCache() {
        copyCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(7, TimeUnit.DAYS)
                .build(
                        new CacheLoader<Path, Path>() {
                            @Override
                            public Path load(Path key) throws Exception {
                                Path p = Utils.resolvePath(key, monitorDir, moveDir);
                                if (Files.exists(p)) return p;
                                throw new Exception("Path does not exist so not cached");
                            }
                        });

        try {
            Files.walkFileTree(moveDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isHidden(file)) return FileVisitResult.CONTINUE;
                    copyCache.put(Utils.resolvePath(file, moveDir, monitorDir), file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            handleException(monitorDir, "Failed to scan directory " + monitorDir + " because {}", e);
        }

    }

    private void moveFile(Path location, Path destination) throws IOException {
        logger.debug("Renaming to " + destination);
        Files.createDirectories(destination.getParent());
        Files.move(location, destination);
    }

    private void processFiles(Collection<Path> paths, LocalDateTime currentTime) {
        for (Path path : paths) {
            try {
                if (processFile(path, currentTime)) {
                    fileSizes.remove(path);
                    lastModified.remove(path);
                    timeDiscovered.remove(path);
                }
            } catch (IOException | TimeoutException | JAXBException e) {
                handleException(path, "Failed to process file " + path + " because: {}", e);
            }
        }
    }

    private void sendBurstMessage(AMQP.BasicProperties basicProperties, byte[] message) throws IOException {
        if (channel != null) channel.basicPublish(exchangeName, burstQueue, basicProperties, message);
        else {
            // Create a one time channel
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);
            channel.basicPublish(exchangeName, burstQueue, basicProperties, message);
            try {
                channel.close();
            } catch (TimeoutException ignored) {}
        }
    }
}
