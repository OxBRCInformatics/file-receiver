package ox.softeng.gel.filereceive;

import ox.softeng.gel.filereceive.config.Folder;
import ox.softeng.gel.filereceive.config.Headers;

import com.rabbitmq.client.Channel;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @since 29/03/2016
 */
public class FolderMonitorTest {

    private static final String TEMP_TEST_FOLDER = "/tmp/filereceive";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private FolderMonitor fm;
    private Folder folder;
    private UUID testName;
    private Long time;

    @Test
    public void checkingForFilesToHandleShouldFindFilesAsExpected() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, null, null, -1L);
        fm.scanMonitorDirectory(time);

        Set<Path> files = fm.checkForFilesToHandle(time);
        assertTrue("should contain no files", files.isEmpty());

        Path tempFile = Files.createTempFile(fm.monitorDir, "test.", ".xml");
        fm.scanMonitorDirectory(time);

        fm.refreshTime = 5000L;
        files = fm.checkForFilesToHandle(time);
        assertTrue("should contain no files when refresh time is 5", files.isEmpty());

        fm.refreshTime = -1L;
        files = fm.checkForFilesToHandle(time);
        assertEquals("should contain 1 file when refresh time is -1", 1, files.size());
        assertTrue("should be the temp file", files.contains(tempFile));
        assertEquals("Should have 1 file in files sizes", 1, fm.fileSizes.size());
        assertEquals("Should have 1 file in modified", 1, fm.lastModified.size());
        assertEquals("Should have 1 file in discovered", 1, fm.timeDiscovered.size());

        assertTrue("temp file should be deleted", Files.deleteIfExists(tempFile));
        files = fm.checkForFilesToHandle(time);
        assertTrue("should contain no files", files.isEmpty());
        assertTrue("Should have no files", fm.fileSizes.isEmpty());
        assertTrue("Should have no files", fm.lastModified.isEmpty());
        assertTrue("Should have no files", fm.timeDiscovered.isEmpty());
    }

    @Test
    public void foldersShouldBeGeneratedWhenMissing() throws Exception {

        assertFalse("The search folder should not exist", Files.exists(Paths.get(TEMP_TEST_FOLDER, testName.toString(), "find")));
        assertFalse("The move folder should not exist", Files.exists(Paths.get(TEMP_TEST_FOLDER, testName.toString(), "move")));

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, null, null, 0L);

        assertTrue("The search folder should exist", Files.exists(fm.monitorDir));
        assertTrue("The move folder should exist", Files.exists(fm.moveDir));
    }

    @Test
    public void foldersShouldBeGeneratedWhenMissingWhenPathsIncludeExtraSlash() throws Exception {

        assertFalse("The search folder should not exist", Files.exists(Paths.get(TEMP_TEST_FOLDER, testName.toString(), "find")));
        assertFalse("The move folder should not exist", Files.exists(Paths.get(TEMP_TEST_FOLDER, testName.toString(), "move")));

        folder.setFolderPath("/" + testName.toString() + "/find");
        folder.setMoveDestination("/" + testName.toString() + "/move");
        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, null, null, 0L);

        assertTrue("The search folder should exist", Files.exists(fm.monitorDir));
        assertTrue("The move folder should exist", Files.exists(fm.moveDir));
    }

    @Test
    public void handlingFileShouldCopeWithNestedFolders() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, "testexc", "burst-queue", -1L);
        fm.channel = mock(Channel.class);

        Path tempFolder = Files.createTempDirectory(fm.monitorDir, "test.dir.");
        Path tempFile2 = Files.createTempFile(tempFolder, "test2.", ".xml");
        String filename = tempFile2.getFileName().toString();
        String ext = com.google.common.io.Files.getFileExtension(filename);
        String renameFilename = filename.replace("." + ext, "." + time + "." + ext);

        boolean result = fm.processFile(tempFile2, time);

        verify(fm.channel).basicPublish(eq("testexc"), eq("burst-queue"), any(), any());
        verify(fm.channel).basicPublish(eq("testexc"), eq("test-queue"), any(), any());
        assertTrue("Should have processed correctly", result);
        assertFalse("tempfile should no longer exist", Files.exists(tempFile2));

        assertTrue("moved temp file should exist", Files.exists(Paths.get(fm.moveDir.toString(), tempFolder.getFileName().toString(),
                                                                          renameFilename)));
    }

    @Test
    public void handlingFileShouldHandleSingleFile() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, "testexc", "burst-queue", -1L);
        fm.channel = mock(Channel.class);
        Path tempFile = Files.createTempFile(fm.monitorDir, "test.", ".xml");
        String content = "hello";
        Files.write(tempFile, content.getBytes());
        String filename = tempFile.getFileName().toString();
        String ext = com.google.common.io.Files.getFileExtension(filename);
        String renameFilename = filename.replace("." + ext, "." + time + "." + ext);

        boolean result = fm.processFile(tempFile, time);
        verify(fm.channel).basicPublish(eq("testexc"), eq("burst-queue"), any(), any());
        verify(fm.channel).basicPublish(eq("testexc"), eq("test-queue"), any(), any());
        assertTrue("Should have processed correctly", result);
        assertFalse("tempfile should no longer exist", Files.exists(tempFile));

        assertTrue("moved temp file should exist", Files.exists(Paths.get(fm.moveDir.toString(), renameFilename)));
    }

    @Test
    public void handlingFileShouldThrowExceptionWhenNoFileFound() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, "testexc", "burst-queue", -1L);
        fm.channel = mock(Channel.class);
        Path tempFile = Files.createTempFile(fm.monitorDir, "test.", ".xml");

        Files.delete(tempFile);

        expectedException.expect(NoSuchFileException.class);
        fm.processFile(tempFile, time);
    }

    @Test
    public void handlingFileShouldWorkWhenFileHasAlreadyBeenProcessed() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, "testexc", "burst-queue", -1L);
        fm.channel = mock(Channel.class);
        Path tempFile = Files.createTempFile(fm.monitorDir, "test.", ".xml");
        String content = "hello";
        Files.write(tempFile, content.getBytes());
        String filename = tempFile.getFileName().toString();
        String ext = com.google.common.io.Files.getFileExtension(filename);
        String renameFilename = filename.replace("." + ext, "." + time + "." + ext);

        boolean result = fm.processFile(tempFile, time);
        verify(fm.channel).basicPublish(eq("testexc"), eq("burst-queue"), any(), any());
        verify(fm.channel).basicPublish(eq("testexc"), eq("test-queue"), any(), any());
        assertTrue("Should have processed correctly", result);
        assertFalse("tempfile should no longer exist", Files.exists(tempFile));
        assertTrue("moved temp file should exist", Files.exists(Paths.get(fm.moveDir.toString(), renameFilename)));

        Files.createFile(tempFile);
        time = System.currentTimeMillis();
        result = fm.processFile(tempFile, time);
        verify(fm.channel, times(2)).basicPublish(eq("testexc"), eq("burst-queue"), any(), any());
        verify(fm.channel, times(2)).basicPublish(eq("testexc"), eq("test-queue"), any(), any());
        assertTrue("Should have processed correctly", result);
        assertFalse("tempfile should no longer exist", Files.exists(tempFile));
        assertTrue("moved temp file should exist", Files.exists(Paths.get(fm.moveDir.toString(), renameFilename)));
    }

    @Test
    public void scanningDirectoriesShouldPickupExpectedFiles() throws Exception {

        fm = new FolderMonitor(null, TEMP_TEST_FOLDER, folder, null, null, 0L);

        fm.scanMonitorDirectory(System.currentTimeMillis());

        assertTrue("Should have no files", fm.fileSizes.isEmpty());
        assertTrue("Should have no files", fm.lastModified.isEmpty());
        assertTrue("Should have no files", fm.timeDiscovered.isEmpty());

        Path tempFile = Files.createTempFile(fm.monitorDir, "test.", ".xml");

        fm.scanMonitorDirectory(time);

        assertFalse("Should have a file", fm.fileSizes.isEmpty());
        assertFalse("Should have a file", fm.lastModified.isEmpty());
        assertFalse("Should have a file", fm.timeDiscovered.isEmpty());

        assertEquals("Should have a file with correct size", Files.size(tempFile), fm.fileSizes.get(tempFile).longValue());
        assertEquals("Should have a file with same modified date", Files.getLastModifiedTime(tempFile), fm.lastModified.get(tempFile));
        assertEquals("Should have the correct time discovered", time, fm.timeDiscovered.get(tempFile));

        fm.scanMonitorDirectory(time);

        assertEquals("Should still have 1 file", 1, fm.fileSizes.size());
        assertEquals("Should still have 1 file", 1, fm.lastModified.size());
        assertEquals("Should still have 1 file", 1, fm.timeDiscovered.size());

        assertEquals("Should have a file with correct size", Files.size(tempFile), fm.fileSizes.get(tempFile).longValue());
        assertEquals("Should have a file with same modified date", Files.getLastModifiedTime(tempFile), fm.lastModified.get(tempFile));
        assertEquals("Should have the correct time discovered", time, fm.timeDiscovered.get(tempFile));

        Path tempFolder = Files.createTempDirectory(fm.monitorDir, "test.dir.");
        Path tempFile2 = Files.createTempFile(tempFolder, "test2.", ".xml");

        fm.scanMonitorDirectory(time);

        assertEquals("Should still have 2 files", 2, fm.fileSizes.size());
        assertEquals("Should still have 2 files", 2, fm.lastModified.size());
        assertEquals("Should still have 2 files", 2, fm.timeDiscovered.size());

        assertEquals("Should have a file with correct size", Files.size(tempFile), fm.fileSizes.get(tempFile).longValue());
        assertEquals("Should have a file with same modified date", Files.getLastModifiedTime(tempFile), fm.lastModified.get(tempFile));
        assertEquals("Should have the correct time discovered", time, fm.timeDiscovered.get(tempFile));

        assertEquals("Should have another file with correct size", Files.size(tempFile2), fm.fileSizes.get(tempFile2).longValue());
        assertEquals("Should have another file with same modified date", Files.getLastModifiedTime(tempFile2), fm.lastModified.get(tempFile2));
        assertEquals("Should have the correct time discovered again", time, fm.timeDiscovered.get(tempFile2));

        assertFalse("Should not have a size key for the folder", fm.fileSizes.containsKey(tempFolder));
        assertFalse("Should not have a modified key for the folder", fm.lastModified.containsKey(tempFolder));
        assertFalse("Should not have a discovered key for the folder", fm.timeDiscovered.containsKey(tempFolder));

    }

    @Before
    public void setUp() throws Exception {
        testName = UUID.randomUUID();
        folder = new Folder();
        folder.setFolderPath(testName.toString() + "/find");
        folder.setMoveDestination(testName.toString() + "/move");
        folder.setQueueName("test-queue");
        folder.setHeaders(new Headers());

        time = System.currentTimeMillis();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {

        Path path = Paths.get(TEMP_TEST_FOLDER);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw exc;
                }
            }
        });

    }
}