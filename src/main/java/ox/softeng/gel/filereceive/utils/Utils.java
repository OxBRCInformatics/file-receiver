package ox.softeng.gel.filereceive.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @since 19/04/2016
 */
public class Utils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss_SSS");

    public static Path resolvePath(Path path, Path location, Path destination) throws IOException {
        Path relativeToLocation = location.relativize(path);
        return destination.resolve(relativeToLocation);
    }

    public static Path resolvePath(Path path, Path location, Path destination, LocalDateTime currentTime) throws IOException {
        Path resolved = resolvePath(path, location, destination);

        String filename = resolved.getFileName().toString();
        String ext = com.google.common.io.Files.getFileExtension(filename);
        String renameFilename = filename.replace("." + ext, "." + currentTime.format(DATE_TIME_FORMATTER) + "." + ext);

        return resolved.resolveSibling(renameFilename);
    }

}
