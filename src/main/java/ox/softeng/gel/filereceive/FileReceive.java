package ox.softeng.gel.filereceive;

import ox.softeng.gel.filereceive.config.Configuration;
import ox.softeng.gel.filereceive.config.Context;
import ox.softeng.gel.filereceive.config.Folder;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.cli.*;
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
    private static final CommandLineParser parser = new DefaultParser();
    private String burstQueue;
    private Configuration config;
    private String exchangeName;
    private ConnectionFactory factory;
    private Long refreshTime;

    public FileReceive(String configFilename, String rabbitMqHost, Integer rabbitMqPort, String exchangeName, String burstQueue, Long refreshTime)
            throws JAXBException, IOException, TimeoutException {

        logger.info("Starting application version {}", System.getProperty("applicationVersion"));

        // Output env version from potential dockerfile environment
        if (System.getenv("FILE_RECEIVER_VERSION") != null)
            logger.info("Docker container build version {}", System.getenv("FILE_RECEIVER_VERSION"));

        Integer port = rabbitMqPort != null ? rabbitMqPort : ConnectionFactory.DEFAULT_AMQP_PORT;

        logger.info("Using:\n" +
                    " - Config file: {}\n" +
                    " - RabbitMQ Server Host: {}:{}\n" +
                    " - Exchange Name: {}\n" +
                    " - BuRST Queue Binding: {}\n" +
                    " - Refresh Time: {} seconds", configFilename, rabbitMqHost, port, exchangeName, burstQueue, refreshTime);

        this.refreshTime = refreshTime;
        this.exchangeName = exchangeName;
        this.burstQueue = burstQueue;

        factory = new ConnectionFactory();
        factory.setHost(rabbitMqHost);
        factory.setPort(port);
        factory.setAutomaticRecoveryEnabled(true);

        File configFile = new File(configFilename);
        Unmarshaller unmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
        config = (Configuration) unmarshaller.unmarshal(configFile);
    }

    private static Options defineMainOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("c").longOpt("config")
                        .argName("FILE")
                        .hasArg().required()
                        .desc("The config file defining the monitor config")
                        .build());
        options.addOption(
                Option.builder("r").longOpt("rabbitmq-server")
                        .argName("RABBITMQ_HOST")
                        .hasArg().required()
                        .desc("Hostname for the RabbitMQ server")
                        .build());
        options.addOption(
                Option.builder("p").longOpt("rabbitmq-port")
                        .argName("RABBITMQ_PORT")
                        .hasArg()
                        .desc("[Optional] Port for the RabbitMQ server, default is 5672")
                        .type(Number.class)
                        .build());
        options.addOption(
                Option.builder("e").longOpt("exchange")
                        .argName("EXCHANGE")
                        .hasArg().required()
                        .desc("Exchange on the RabbitMQ server to send files to.\n" +
                              "Queues for the folders are designated in the config file")
                        .build());
        options.addOption(
                Option.builder("b").longOpt("burst-binding")
                        .argName("BURST")
                        .hasArg().required()
                        .desc("Binding key for the BuRST queue at the named exchange")
                        .build());
        options.addOption(
                Option.builder("t").longOpt("refresh-time")
                        .argName("TIME")
                        .hasArg().required()
                        .desc("Refresh time for monitoring in seconds")
                        .type(Number.class)
                        .build());
        return options;
    }

    private static boolean hasSimpleOptions(String[] args) {

        Options options = new Options();
        OptionGroup group = new OptionGroup();
        group.addOption(Option.builder("h").longOpt("help").build());
        group.addOption(Option.builder("v").longOpt("version").build());
        options.addOptionGroup(group);

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("h")) help();
            if (line.hasOption("v")) System.out.println(version());
            ;
            return true;
        } catch (ParseException ignored) {}
        return false;
    }

    private static void help() {
        HelpFormatter formatter = new HelpFormatter();

        String header = "Receive files by monitoring folders and forwarding files to a RabbitMQ system.\n" +
                        "Sends success messages and error messages to a BuRST system.\n\n";
        String footer = "\n" + version() + "\n\nPlease report issues at https://github.com/oxbrcinformatics/file-receiver/issues\n";

        formatter.printHelp(120,
                            "file-receiver -c <FILE> -r <RABBITMQ_HOST> [-p <RABBITMQ_PORT>] -e <EXCHANGE> -b <BURST> -t <TIME>",
                            header, defineMainOptions(), footer, false);
    }

    private void startMonitors() throws IOException, TimeoutException {

        for (Context c : config.getContext()) {
            logger.info("Starting {} folder monitors for context {}", c.getFolder().size(), c.getPath());

            // One connection for each context
            Connection connection = factory.newConnection();

            Long refresh = c.getRefreshFrequency() == null ? refreshTime : c.getRefreshFrequency().longValue();
            String exchange = c.getExchange() == null ? exchangeName : c.getExchange();

            int count = 0;
            for (Folder f : c.getFolder()) {
                Path monitorDir = Paths.get(c.getPath(), f.getMonitorDirectory());
                try {
                    FolderMonitor fm = new FolderMonitor(connection, c.getPath(), f, exchange, burstQueue, refresh);
                    logger.debug("Created folder monitor for {}", monitorDir);

                    new Thread(fm).start();
                    count++;
                } catch (JAXBException | IOException e) {
                    logger.error("Could not create folder monitor for folder {} because: {}",
                                 monitorDir, e.getMessage());
                    e.printStackTrace();
                }
                logger.debug("Started monitoring for folder: {}", monitorDir);
            }
            logger.info("Started {} folder monitors for context {}", count, c.getPath());
        }
    }

    private static String version() {
        return "file-receiver version: \"" + FileReceive.class.getPackage().getSpecificationVersion() + "\"\n" +
               "Java Version: \"" + System.getProperty("java.version") + "\"";

    }

    public static void main(String[] args) {

        if (hasSimpleOptions(args)) System.exit(0);

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(defineMainOptions(), args);

            long start = System.currentTimeMillis();

            try {
                FileReceive fr = new FileReceive(line.getOptionValue('c'),
                                                 line.getOptionValue('r'),
                                                 (Integer) line.getParsedOptionValue("p"),
                                                 line.getOptionValue('e'),
                                                 line.getOptionValue('b'),
                                                 (Long) line.getParsedOptionValue("t"));
                fr.startMonitors();

                logger.info("File receiver started in {}ms", System.currentTimeMillis() - start);
            } catch (JAXBException ex) {
                logger.error("Could not create file receiver because of JAXBException: {}", ex.getLinkedException().getMessage());
            } catch (IOException | TimeoutException ex) {
                logger.error("Could not create file receiver because: {}", ex.getMessage());
                ex.printStackTrace();
            }
        } catch (ParseException exp) {
            logger.error("Could not start file-receiver because of ParseException: " + exp.getMessage());
            help();
        }
    }
}
