/**
 * Academic Use Licence
 *
 * These licence terms apply to all licences granted by
 * OXFORD UNIVERSITY INNOVATION LIMITED whose administrative offices are at
 * University Offices, Wellington Square, Oxford OX1 2JD, United Kingdom ("OUI")
 * for use of File Receiver, a simple tool for monitoring SFTP folders and
 * forwarded incoming messages into queues for processing by a distributed
 * software architecture ("the Software") through this website
 * https://github.com/OxBRCInformatics/file-receiver (the "Website").
 *
 * PLEASE READ THESE LICENCE TERMS CAREFULLY BEFORE DOWNLOADING THE SOFTWARE
 * THROUGH THIS WEBSITE. IF YOU DO NOT AGREE TO THESE LICENCE TERMS YOU SHOULD NOT
 * [REQUEST A USER NAME AND PASSWORD OR] DOWNLOAD THE SOFTWARE.
 *
 * THE SOFTWARE IS INTENDED FOR USE BY ACADEMICS CARRYING OUT RESEARCH AND NOT FOR
 * USE BY CONSUMERS OR COMMERCIAL BUSINESSES.
 *
 * 1. Academic Use Licence
 *
 *   1.1 The Licensee is granted a limited non-exclusive and non-transferable
 *       royalty free licence to download and use the Software provided that the
 *       Licensee will:
 *
 *       (a) limit their use of the Software to their own internal academic
 *           non-commercial research which is undertaken for the purposes of
 *           education or other scholarly use;
 *
 *       (b) not use the Software for or on behalf of any third party or to
 *           provide a service or integrate all or part of the Software into a
 *           product for sale or license to third parties;
 *
 *       (c) use the Software in accordance with the prevailing instructions and
 *           guidance for use given on the Website and comply with procedures on
 *           the Website for user identification, authentication and access;
 *
 *       (d) comply with all applicable laws and regulations with respect to their
 *           use of the Software; and
 *
 *       (e) ensure that the Copyright Notice (c) 2016, Oxford University
 *           Innovation Ltd." appears prominently wherever the Software is
 *           reproduced and is referenced or cited with the Copyright Notice when
 *           the Software is described in any research publication or on any
 *           documents or other material created using the Software.
 *
 *   1.2 The Licensee may only reproduce, modify, transmit or transfer the
 *       Software where:
 *
 *       (a) such reproduction, modification, transmission or transfer is for
 *           academic, research or other scholarly use;
 *
 *       (b) the conditions of this Licence are imposed upon the receiver of the
 *           Software or any modified Software;
 *
 *       (c) all original and modified Source Code is included in any transmitted
 *           software program; and
 *
 *       (d) the Licensee grants OUI an irrevocable, indefinite, royalty free,
 *           non-exclusive unlimited licence to use and sub-licence any modified
 *           Source Code as part of the Software.
 *
 *     1.3 OUI reserves the right at any time and without liability or prior
 *         notice to the Licensee to revise, modify and replace the functionality
 *         and performance of the access to and operation of the Software.
 *
 *     1.4 The Licensee acknowledges and agrees that OUI owns all intellectual
 *         property rights in the Software. The Licensee shall not have any right,
 *         title or interest in the Software.
 *
 *     1.5 This Licence will terminate immediately and the Licensee will no longer
 *         have any right to use the Software or exercise any of the rights
 *         granted to the Licensee upon any breach of the conditions in Section 1
 *         of this Licence.
 *
 * 2. Indemnity and Liability
 *
 *   2.1 The Licensee shall defend, indemnify and hold harmless OUI against any
 *       claims, actions, proceedings, losses, damages, expenses and costs
 *       (including without limitation court costs and reasonable legal fees)
 *       arising out of or in connection with the Licensee's possession or use of
 *       the Software, or any breach of these terms by the Licensee.
 *
 *   2.2 The Software is provided on an "as is" basis and the Licensee uses the
 *       Software at their own risk. No representations, conditions, warranties or
 *       other terms of any kind are given in respect of the the Software and all
 *       statutory warranties and conditions are excluded to the fullest extent
 *       permitted by law. Without affecting the generality of the previous
 *       sentences, OUI gives no implied or express warranty and makes no
 *       representation that the Software or any part of the Software:
 *
 *       (a) will enable specific results to be obtained; or
 *
 *       (b) meets a particular specification or is comprehensive within its field
 *           or that it is error free or will operate without interruption; or
 *
 *       (c) is suitable for any particular, or the Licensee's specific purposes.
 *
 *   2.3 Except in relation to fraud, death or personal injury, OUI"s liability to
 *       the Licensee for any use of the Software, in negligence or arising in any
 *       other way out of the subject matter of these licence terms, will not
 *       extend to any incidental or consequential damages or losses, or any loss
 *       of profits, loss of revenue, loss of data, loss of contracts or
 *       opportunity, whether direct or indirect.
 *
 *   2.4 The Licensee hereby irrevocably undertakes to OUI not to make any claim
 *       against any employee, student, researcher or other individual engaged by
 *       OUI, being a claim which seeks to enforce against any of them any
 *       liability whatsoever in connection with these licence terms or their
 *       subject-matter.
 *
 * 3. General
 *
 *   3.1 Severability - If any provision (or part of a provision) of these licence
 *       terms is found by any court or administrative body of competent
 *       jurisdiction to be invalid, unenforceable or illegal, the other
 *       provisions shall remain in force.
 *
 *   3.2 Entire Agreement - These licence terms constitute the whole agreement
 *       between the parties and supersede any previous arrangement, understanding
 *       or agreement between them relating to the Software.
 *
 *   3.3 Law and Jurisdiction - These licence terms and any disputes or claims
 *       arising out of or in connection with them shall be governed by, and
 *       construed in accordance with, the law of England. The Licensee
 *       irrevocably submits to the exclusive jurisdiction of the English courts
 *       for any dispute or claim that arises out of or in connection with these
 *       licence terms.
 *
 * If you are interested in using the Software commercially, please contact
 * Oxford University Innovation Limited to negotiate a licence.
 * Contact details are enquiries@innovation.ox.ac.uk quoting reference 14422.
 */
package ox.softeng.gel.filereceive;

import ox.softeng.burst.domain.SeverityEnum;
import ox.softeng.burst.xml.MessageDTO;
import ox.softeng.gel.filereceive.config.Configuration;
import ox.softeng.gel.filereceive.config.Context;
import ox.softeng.gel.filereceive.config.Folder;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeoutException;

import static ox.softeng.gel.filereceive.utils.Utils.loadConfig;

public class FileReceive {

    private static final Logger logger = LoggerFactory.getLogger(FileReceive.class);
    private static final CommandLineParser parser = new DefaultParser();
    private String burstQueue;
    private Configuration config;
    private String exchangeName;
    private ConnectionFactory factory;
    private Long refreshTime;

    public FileReceive(String configFilename, String rabbitMqHost, Integer rabbitMqPort, String username, String password,
                       String exchangeName, String burstQueue, Long refreshTime)
            throws JAXBException, IOException, TimeoutException {

        logger.info("Starting application version {}", System.getProperty("applicationVersion"));

        // Output env version from potential dockerfile environment
        if (System.getenv("FILE_RECEIVER_VERSION") != null)
            logger.info("Docker container build version {}", System.getenv("FILE_RECEIVER_VERSION"));

        Integer port = rabbitMqPort != null ? rabbitMqPort : ConnectionFactory.DEFAULT_AMQP_PORT;

        logger.info("Using:\n" +
                    " - Config file: {}\n" +
                    " - RabbitMQ Server Host: {}:{}\n" +
                    " - RabbitMQ User: {}:****\n" +
                    " - Exchange Name: {}\n" +
                    " - BuRST Queue Binding: {}\n" +
                    " - Refresh Time: {} seconds", configFilename, rabbitMqHost, port, username, exchangeName, burstQueue, refreshTime);

        this.refreshTime = refreshTime;
        this.exchangeName = exchangeName;
        this.burstQueue = burstQueue;

        factory = new ConnectionFactory();
        factory.setHost(rabbitMqHost);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setAutomaticRecoveryEnabled(true);

        config = loadConfig(configFilename);
    }

    public void generateStartupMessage() throws IOException {

        try {
            StringWriter writer;
            MessageDTO message = new MessageDTO();
            message.setSource("file-receiver");
            message.setDetails("Burst Service starting\n" + version());
            message.setSeverity(SeverityEnum.INFORMATIONAL);
            message.setDateTimeCreated(OffsetDateTime.now(ZoneId.of("UTC")));
            message.setTitle("File Receiver Startup");
            message.addTopic("service");
            message.addTopic("startup");
            message.addTopic("file-receiver");
            message.addMetadata("gmc", "gel");
            message.addMetadata("file_receiver_service_version", version());

            writer = new StringWriter();
            getMarshaller().marshal(message, writer);

            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.deliveryMode(2);
            builder.contentType("text/xml");
            builder.timestamp(Date.from(OffsetDateTime.now(ZoneId.systemDefault()).toInstant()));

            Channel channel = factory.newConnection().createChannel();
            channel.exchangeDeclare(exchangeName, "topic", true);
            channel.basicPublish(exchangeName, burstQueue, builder.build(), writer.toString().getBytes());
            channel.close();

        } catch (JAXBException | TimeoutException ignored) {
        }
    }

    Marshaller getMarshaller() throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(MessageDTO.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
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
                        .desc("[Optional] Port for the RabbitMQ server, default is " + ConnectionFactory.DEFAULT_AMQP_PORT)
                        .type(Number.class)
                        .build());
        options.addOption(
                Option.builder("u").longOpt("rabbitmq-user")
                        .argName("RABBITMQ_USER")
                        .hasArg()
                        .desc("[Optional] User for the RabbitMQ server, default is " + ConnectionFactory.DEFAULT_USER)
                        .build());
        options.addOption(
                Option.builder("w").longOpt("rabbitmq-password")
                        .argName("RABBITMQ_PASSWORD")
                        .hasArg()
                        .desc("[Optional] Password for the RabbitMQ server, default is " + ConnectionFactory.DEFAULT_PASS)
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

    private static String fullVersion() {
        return "file-receiver " + version();
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
            if (line.hasOption("v")) System.out.println(fullVersion());
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

        generateStartupMessage();

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
        return "Version: \"" + FileReceive.class.getPackage().getSpecificationVersion() + "\"\n" +
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
                                                 line.getOptionValue('u', ConnectionFactory.DEFAULT_USER),
                                                 line.getOptionValue('w', ConnectionFactory.DEFAULT_PASS),
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
