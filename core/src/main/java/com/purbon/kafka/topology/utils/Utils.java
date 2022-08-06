package com.purbon.kafka.topology.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils {

  private static final Logger LOGGER = LogManager.getLogger(Utils.class);

  public static Stream<String> asNullableStream(List<String> items) {
    Optional<List<String>> optional = Optional.ofNullable(items);
    return optional.stream().flatMap(Collection::stream);
  }

  public static String readFullFile(Path path) throws IOException {
    return Files.readString(path);
  }

  public static Path filePath(String file, String rootPath) {
    Path mayBeAbsolutePath = Paths.get(file);
    Path path = mayBeAbsolutePath.isAbsolute() ? mayBeAbsolutePath : Paths.get(rootPath, file);
    LOGGER.debug(String.format("Artefact File %s loaded from %s", file, path));
    return path;
  }
}
