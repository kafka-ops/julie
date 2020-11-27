package com.purbon.kafka.topology.utils;

import com.purbon.kafka.topology.backend.FileBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class TestUtils {

  private TestUtils() {
  }

  public static void deleteStateFile() {
    try {
      Files.deleteIfExists(Paths.get(FileBackend.STATE_FILE_NAME));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

}
