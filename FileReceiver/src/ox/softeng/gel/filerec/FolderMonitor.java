package ox.softeng.gel.filerec;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import ox.softeng.burst.domain.Severity;
import ox.softeng.burst.services.MessageDTO;
import ox.softeng.burst.services.MessageDTO.Metadata;
import ox.softeng.gel.filerec.config.Folder;
import ox.softeng.gel.filerec.config.Header;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class FolderMonitor implements Runnable {

    private String contextPath;
    private Path dir;
    private Folder folder;

    private String exchangeName; // "Carfax"
    private String burstQueue; // "noaudit.burst"
    private Long refreshTime;

    private HashMap<String, Long> fileSizes;
    private HashMap<String, Long> lastModified;
    private HashMap<String, Long> timeDiscovered;

    private JAXBContext burstMessageContext;

    private Channel channel;
    private Connection connection;


    public FolderMonitor(Connection connection, String contextPath, Folder folder, String exchangeName, String burstQueue,
                         Long refreshTime) {

        this.connection = connection;
        this.contextPath = contextPath;
        String dirName = contextPath + "/" + folder.getFolderPath();
        dir = Paths.get(dirName.replace("//", "/"));

        String moveDestinationName = contextPath + "/" + folder.getMoveDestination();
        Path moveDestination = Paths.get(moveDestinationName.replace("//", "/"));

        this.folder = folder;

        this.exchangeName = exchangeName; // "Carfax"
        this.burstQueue = burstQueue; // "noaudit.burst"
        this.refreshTime = refreshTime;
        fileSizes = new HashMap<>();
        lastModified = new HashMap<>();
        timeDiscovered = new HashMap<>();

        try {
            burstMessageContext = JAXBContext.newInstance(MessageDTO.class);
        } catch (JAXBException e) {
            System.err.println("FolderMonitor:constructor:-");
            e.printStackTrace();
        }

        try {
            if (!Files.exists(dir)) {
                System.out.println("Folder not exists: " + dir);
                System.out.println("Creating folder: " + dir);
                Files.createDirectories(dir);
            }
            if (!Files.exists(moveDestination)) {
                System.out.println("Folder not exists: " + dir);
                System.out.println("Creating folder: " + dir);
                Files.createDirectories(moveDestination);
            }
            System.out.println("dir type: " + Files.getFileStore(dir).type());
        } catch (IOException e2) {
            System.err.println("FolderMonitor:run:-");
            e2.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            // Only create the channel once as we only need 1 and creation is expensive
            // Also allows the loop to run as long as the channel is open, if the channel closes then the program
            // ends as no point running if the channel is closed
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);

            while (channel.isOpen()) {
                Long currentTime = System.currentTimeMillis();
                // First we'll go through the files that we've already found

                // Keep a copy of the keyset so that we can modify the underlying hashset while iterating.
                Set<String> fileNames = new HashSet<>(timeDiscovered.keySet());
                for (String fileName : fileNames) {
                    //System.out.println("Re-examining file: " + fileName);

                    File f = new File(fileName);
                    // If the file no-longer exists, then remove it
                    if (!f.exists() || f.isDirectory()) {
                        //System.out.println("file no-longer exists!");
                        fileSizes.remove(fileName);
                        lastModified.remove(fileName);
                        timeDiscovered.remove(fileName);
                    } else {
                        Long thisFileLastModified = f.lastModified();
                        Long thisFileSize = f.length();

                        // if it's been modified, then update it
                        if (!thisFileLastModified.equals(lastModified.get(fileName))
                            || !thisFileSize.equals(fileSizes.get(fileName))) {
                            //System.out.println("File modified since last examined.");
                            fileSizes.put(fileName, thisFileSize);
                            lastModified.put(fileName, thisFileLastModified);
                            timeDiscovered.put(fileName, currentTime);

                        } else // It's the same file as we've seen before...
                        {
                            // Only consider it if it has been there for a suitable duration
                            if (timeDiscovered.get(fileName) + refreshTime < currentTime) {
                                //System.out.println("File not modified.");
                                try {
                                    handleNewFile(f.getName());
                                    fileSizes.remove(fileName);
                                    lastModified.remove(fileName);
                                    timeDiscovered.remove(fileName);
                                } catch (IOException | TimeoutException | JAXBException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                // Handle recursive/sub folders
                scanDirectoryFiles(dir.toFile().listFiles(), currentTime);

                try {
                    // Sleep for a second
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void scanDirectoryFiles(File[] files, Long currentTime) {
        // Now we'll look and see if there are any new files to add
        if (files == null || files.length == 0) return;

        for (File f : files) {

            if (f.isDirectory()) {
                scanDirectoryFiles(f.listFiles(), currentTime);
            } else {
                String thisFileAbsolutePath = f.getAbsolutePath();
                // If we've not seen this file before
                if (!lastModified.containsKey(thisFileAbsolutePath)) {
                    Long thisFileLastModified = f.lastModified();
                    Long thisFileSize = f.length();
                    System.out.println("registering file: " + thisFileAbsolutePath);

                    fileSizes.put(thisFileAbsolutePath, thisFileSize);
                    lastModified.put(thisFileAbsolutePath, thisFileLastModified);
                    timeDiscovered.put(thisFileAbsolutePath, currentTime);
                }
            }
        }
    }

    private void handleNewFile(String filename) throws IOException, TimeoutException, JAXBException {
        String fullPath = contextPath + folder.getFolderPath() + "/" + filename;

        Path path = Paths.get(fullPath);

        byte[] message = Files.readAllBytes(path);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("filename", filename);
        headerMap.put("directory", contextPath + folder.getFolderPath());
        headerMap.put("receivedDateTime", OffsetDateTime.now(ZoneId.systemDefault()).toString());
        for (Header h : folder.getHeaders().getHeader()) {
            headerMap.put(h.getKey(), h.getValue());
        }
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();

        // Set headers and other required properties
        builder.headers(headerMap);
        builder.appId("folder_monitor_" + contextPath);
        builder.messageId(fullPath + "_" + headerMap.get("receivedDateTime"));
        builder.timestamp(Date.from(OffsetDateTime.now(ZoneId.systemDefault()).toInstant()));
        if(headerMap.containsKey("type")){
            builder.type(headerMap.get("type").toString());
        }else builder.type("file");
        builder.contentType(determineContentType(filename));

        // Send to folder queue
        channel.basicPublish(exchangeName, folder.getQueueName(), builder.build(), message);
        //String burstMessage = "File received";

        // Send to burst
        channel.basicPublish(exchangeName, burstQueue, builder.build(), getSuccessMessage(filename));
        //System.out.println(" [x] Sent '" + message + "'");

        String newPath = contextPath + folder.getMoveDestination() + "/" + filename;
        File file = new File(fullPath);
        file.renameTo(new File(newPath));

        System.out.println("Processed: " + fullPath);
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

    private byte[] getSuccessMessage(String filename) throws JAXBException {
        MessageDTO burstMessage = new MessageDTO();
        burstMessage.setDateTimeCreated(LocalDateTime.now());
        burstMessage.setSeverity(Severity.NOTICE);
        burstMessage.setSource("Sample tracking system");
        String GMCName = "Unknown GMC";
        for (Header h : folder.getHeaders().getHeader()) {
            if ("GMC".equalsIgnoreCase(h.getKey())) {
                GMCName = h.getValue();
                break;
            }
        }
        burstMessage.setDetails("A file with the name \"" + filename + "\" has been uploaded for sample tracking by " + GMCName);
        burstMessage.addTopic("File Receipt");
        burstMessage.addMetadata(new Metadata("GMC", GMCName));
        burstMessage.addMetadata(new Metadata("File name", filename));

        Marshaller m = burstMessageContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        m.marshal(burstMessage, bas);
        return bas.toByteArray();
    }

}
