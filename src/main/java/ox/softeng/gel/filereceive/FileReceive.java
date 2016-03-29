package ox.softeng.gel.filereceive;

import ox.softeng.gel.filereceive.config.Config;
import ox.softeng.gel.filereceive.config.Context;
import ox.softeng.gel.filereceive.config.Folder;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class FileReceive {

    private static final Logger logger = LoggerFactory.getLogger(FileReceive.class);
    private String burstQueue;
    private Config config;
    private Connection connection;
    private String exchangeName;
    private Long refreshTime;

    public FileReceive(String configFilename, String queueHost, String exchangeName, String burstQueue, Long refreshTime)
            throws JAXBException, IOException, TimeoutException {

        logger.info("Using:\n" +
                    " - Config file: {}\n" +
                    " - Queue Host: {}\n" +
                    " - Exchange Name: {}\n" +
                    " - BuRST Queue: {}\n" +
                    " - Refresh Time: {} seconds", configFilename, queueHost, exchangeName, burstQueue, refreshTime);

        this.refreshTime = refreshTime * 1000; // Convert to ms
        this.exchangeName = exchangeName;
        this.burstQueue = burstQueue;

        // Only create 1 connection from the program, use 1 channel per monitor.
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        connection = factory.newConnection();

        File configFile = new File(configFilename);
        Unmarshaller unmarshaller = JAXBContext.newInstance(Config.class).createUnmarshaller();
        config = (Config) unmarshaller.unmarshal(configFile);
    }

    private void startMonitors() {

        for (Context c : config.getContext()) {
            logger.info("Starting {} folder monitors for context {}", c.getFolder().size(), c.getPath());
            int count = 0;
            for (Folder f : c.getFolder()) {
                Path folderPath = Paths.get(c.getPath(), f.getFolderPath());
                try {
                    FolderMonitor fm = new FolderMonitor(connection, c.getPath(), f, exchangeName, burstQueue, refreshTime);
                    logger.debug("Created folder monitor for {}", folderPath);

                    new Thread(fm).start();
                    count++;
                } catch (JAXBException | IOException e) {
                    logger.error("Could not create folder monitor for folder {} because: {}",
                                 folderPath, e.getMessage());
                    e.printStackTrace();
                }
                logger.debug("Started monitoring for folder: {}", folderPath);
            }
            logger.info("Started {} folder monitors for context {}", count, c.getPath());
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: FileReceive configFile queueHost exchangeName burstQueue refreshTime(s)");
            System.exit(0);
        }
        long start = System.currentTimeMillis();

        String configFilename = args[0];
        String queueHost = args[1];
        String exchangeName = args[2];
        String burstQueue = args[3];
        Long refreshTime = Long.parseLong(args[4]);

        try {
            FileReceive fr = new FileReceive(configFilename, queueHost, exchangeName, burstQueue, refreshTime);
            fr.startMonitors();
        } catch (JAXBException | IOException | TimeoutException e) {
            logger.error("Could not create file receiver because {}", e.getMessage());
            e.printStackTrace();
        }


        logger.info("File receiver started in {}ms", System.currentTimeMillis() - start);
    }
}
