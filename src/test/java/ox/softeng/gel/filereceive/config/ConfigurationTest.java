package ox.softeng.gel.filereceive.config;

import ox.softeng.gel.filereceive.utils.Utils;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @since 19/04/2016
 */
public class ConfigurationTest {

    @Test
    public void testLoadingMoreContextLevelHeadersConfig() throws Exception {

        Configuration config = Utils.loadConfig("src/test/resources/config/complicated_config.xml");
        assertNotNull("Should have loaded", config);

        assertEquals("Should have 2 contexts", 2, config.context.size());
        Context context = config.context.get(0);
        assertEquals("with context path", "/sftp-folders", context.path);


        assertEquals("context has 3 headers", 2, context.headers.header.size());

        Header header = context.headers.header.get(0);
        assertEquals("header key", "myHeader1", header.key);
        assertEquals("header value", "topValue1", header.value);

        header = context.headers.header.get(1);
        assertEquals("header key", "topHeader", header.key);
        assertEquals("header value", "topValue", header.value);

        assertEquals("context should have 2 folders", 2, context.folder.size());
        Folder folder = context.folder.get(0);

        assertEquals("folder path", "/example-drop", folder.monitorDirectory);
        assertEquals("move path", "/config", folder.moveDirectory);
        assertEquals("queue", "noaudit.sample-tracking", folder.bindingKey);
        assertEquals("3 headers", 3, folder.headers.header.size());

        header = folder.headers.header.get(0);
        assertEquals("header key", "myHeader1", header.key);
        assertEquals("header value", "myValue1", header.value);

        header = folder.headers.header.get(1);
        assertEquals("header key", "myHeader2", header.key);
        assertEquals("header value", "myValue2", header.value);

        header = folder.headers.header.get(2);
        assertEquals("header key", "topHeader", header.key);
        assertEquals("header value", "topValue", header.value);

        folder = context.folder.get(1);

        assertEquals("folder path", "/example-drop2", folder.monitorDirectory);
        assertEquals("move path", "/config", folder.moveDirectory);
        assertEquals("queue", "audit.sample-tracking", folder.bindingKey);
        assertEquals("2 headers", 2, folder.headers.header.size());
        assertEquals("folder action", "COPY", folder.getAction().value());
        assertEquals("folder exchange", "test", folder.getExchange());
        assertEquals("folder refresh", new BigInteger("5"), folder.getRefreshFrequency());

        header = folder.headers.header.get(0);
        assertEquals("header key", "myHeader1", header.key);
        assertEquals("header value", "topValue1", header.value);

        header = folder.headers.header.get(1);
        assertEquals("header key", "topHeader", header.key);
        assertEquals("header value", "topValue", header.value);
    }

    @Test
    public void testLoadingXmlIntoConfig() throws Exception {

        Path configPath = Paths.get("src/test/resources/config/config.xml");
        assertTrue("Must have a config path", Files.exists(configPath));


        Unmarshaller unmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
        Configuration config = (Configuration) unmarshaller.unmarshal(configPath.toFile());
        assertNotNull("Should have loaded", config);

        assertEquals("Should have 1 context", 1, config.context.size());
        Context context = config.context.get(0);
        assertEquals("with context path", "/sftp-folders", context.path);

        assertEquals("context should have 2 folders", 2, context.folder.size());
        Folder folder = context.folder.get(0);

        assertEquals("folder path", "/example-drop", folder.monitorDirectory);
        assertEquals("move path", "/config", folder.moveDirectory);
        assertEquals("queue", "noaudit.sample-tracking", folder.bindingKey);
        assertEquals("2 headers", 2, folder.headers.header.size());

        Header header = folder.headers.header.get(0);
        assertEquals("header key", "myHeader1", header.key);
        assertEquals("header value", "myValue1", header.value);
    }
}