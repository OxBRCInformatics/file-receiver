package ox.softeng.gel.filereceive.config;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @since 29/03/2016
 */
public class ConfigTest {

    @Test
    public void testLoadingXmlIntoConfig() throws Exception {

        Path configPath = Paths.get("src/test/resources/config/config.xml");
        assertTrue("Must have a config path", Files.exists(configPath));


        Unmarshaller unmarshaller = JAXBContext.newInstance(Config.class).createUnmarshaller();
        Config config = (Config) unmarshaller.unmarshal(configPath.toFile());
        assertNotNull("Should have loaded", config);

        assertEquals("Should have 1 context", 1, config.context.size());
        Context context = config.context.get(0);
        assertEquals("with context path", "/sftp-folders", context.path);

        assertEquals("context should have 2 folders", 2, context.folder.size());
        Folder folder = context.folder.get(0);

        assertEquals("folder path", "/example-drop", folder.folderPath);
        assertEquals("move path", "/config", folder.moveDestination);
        assertEquals("queue", "noaudit.sample-tracking", folder.queueName);
        assertEquals("2 headers", 2, folder.headers.header.size());

        Header header = folder.headers.header.get(0);
        assertEquals("header key", "myHeader1", header.key);
        assertEquals("header value", "myValue1", header.value);
    }

}