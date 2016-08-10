package ox.softeng.gel.filereceive.utils;

import ox.softeng.gel.filereceive.config.Configuration;
import ox.softeng.gel.filereceive.config.Header;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 19/04/2016
 */
public class Utils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss_SSS");

    public static Configuration loadConfig(String configFilename) throws JAXBException {
        File configFile = new File(configFilename);
        Unmarshaller unmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();
        Configuration config = (Configuration) unmarshaller.unmarshal(configFile);

        config.getContext().forEach(context -> {
            if (context.getHeaders() != null && !context.getHeaders().getHeader().isEmpty()) {
                final List<Header> headers = context.getHeaders().getHeader();
                context.getFolder().forEach(folder -> {
                    if (folder.getHeaders() != null && !folder.getHeaders().getHeader().isEmpty()) {
                        final List<String> folderKeys = folder.getHeaders().getHeader().stream()
                                .map(Header::getKey).collect(Collectors.toList());

                        headers.forEach(header -> {
                            if (!folderKeys.contains(header.getKey())) {
                                folder.getHeaders().getHeader().add(header);
                            }
                        });
                    } else {
                        folder.setHeaders(context.getHeaders());
                    }
                });
            }
        });

        return config;
    }

    public static Path resolvePath(Path path, Path location, Path destination, LocalDateTime currentTime) throws IOException {
        Path resolved = resolvePath(path, location, destination);

        String filename = resolved.getFileName().toString();
        String ext = com.google.common.io.Files.getFileExtension(filename);
        String renameFilename = filename.replace("." + ext, "." + currentTime.format(DATE_TIME_FORMATTER) + "." + ext);

        return resolved.resolveSibling(renameFilename);
    }

    public static Path resolvePath(Path path, Path location, Path destination) throws IOException {
        Path relativeToLocation = location.relativize(path);
        return destination.resolve(relativeToLocation);
    }

}
