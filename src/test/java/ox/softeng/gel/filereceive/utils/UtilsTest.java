package ox.softeng.gel.filereceive.utils;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @since 19/04/2016
 */
public class UtilsTest {

    private static final String TEMP_TEST_FOLDER = "/tmp/filereceive";
    private Path destination;
    private Path location;
    private UUID testName;
    private LocalDateTime time;

    @Test
    public void resolvePath() throws Exception {
        String testPath = "test2.xml";
        Path tempFile = location.resolve(testPath);
        Path result = Utils.resolvePath(tempFile, location, destination);

        System.out.println(tempFile);
        System.out.println(result);
        assertEquals("Simple file should resolve", TEMP_TEST_FOLDER + "/" + testName + "/move/" + testPath, result.toString());

        testPath = "sub/anothersub/test.xml";
        tempFile = location.resolve(testPath);
        result = Utils.resolvePath(tempFile, location, destination);
        System.out.println(tempFile);
        System.out.println(result);
        assertEquals("Simple file should resolve", TEMP_TEST_FOLDER + "/" + testName + "/move/" + testPath, result.toString());

    }

    @Test
    public void resolvePathWithTimestamp() throws Exception {
        String testPath = "test2";
        LocalDateTime now = LocalDateTime.now();

        Path tempFile = location.resolve(testPath + ".xml");
        Path result = Utils.resolvePath(tempFile, location, destination, now);

        System.out.println(tempFile);
        System.out.println(result);
        assertEquals("Simple file should resolve",
                     TEMP_TEST_FOLDER + "/" + testName + "/move/" + testPath + "." + now.format(Utils.DATE_TIME_FORMATTER) + ".xml",
                     result.toString());

        testPath = "sub/anothersub/test";
        tempFile = location.resolve(testPath + ".xml");
        result = Utils.resolvePath(tempFile, location, destination, now);
        System.out.println(tempFile);
        System.out.println(result);
        assertEquals("Simple file should resolve",
                     TEMP_TEST_FOLDER + "/" + testName + "/move/" + testPath + "." + now.format(Utils.DATE_TIME_FORMATTER) + ".xml",
                     result.toString());

    }

    @Before
    public void setUp() throws Exception {

        testName = UUID.randomUUID();

        location = Paths.get(TEMP_TEST_FOLDER, testName.toString() + "/find");
        destination = Paths.get(TEMP_TEST_FOLDER, testName.toString() + "/move");
        System.out.println(location);
        System.out.println(destination);


    }

}