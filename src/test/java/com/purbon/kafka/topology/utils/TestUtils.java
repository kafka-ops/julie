package com.purbon.kafka.topology.utils;

import com.purbon.kafka.topology.backend.FileBackend;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class TestUtils {

  private TestUtils() {}

  public static void deleteStateFile() {
    try {
      Files.deleteIfExists(Paths.get(FileBackend.STATE_FILE_NAME));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getResourceFilename(final String resource) {
    return getResourceFile(resource).toString();
  }

  public static File getResourceFile(final String resource) {
    final URL resourceUrl = TestUtils.class.getResource(resource);
    try {
      return Paths.get(resourceUrl.toURI()).toFile();
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
