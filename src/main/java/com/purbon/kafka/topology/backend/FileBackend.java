package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.BackendController.STATE_FILE_NAME;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.utils.JSON;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileBackend implements Backend {

  private static final Logger LOGGER = LogManager.getLogger(FileBackend.class);

  // Use FileWriter instead of RandomAccessFile due to
  // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154
  private FileWriter writer;

  public FileBackend() {
    this.writer = null;
  }

  @Override
  public void createOrOpen() {
    createOrOpen(Mode.APPEND);
  }

  @Override
  public void createOrOpen(Mode mode) {
    try {
      if (this.writer != null) writer.close();
      this.writer = new FileWriter(STATE_FILE_NAME, !Mode.TRUNCATE.equals(mode));
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void save(BackendState state) throws IOException {
    writeText(state.asPrettyJson());
  }

  @Override
  public BackendState load() throws IOException {
    Path filePath = Paths.get(STATE_FILE_NAME);
    if (Files.size(filePath) == 0) { // if we are loading when there is no file or is empty.
      return new BackendState();
    }
    return load(filePath);
  }

  BackendState load(Path stateFilePath) throws IOException {
    String backendStateAsJsonString = Files.readString(stateFilePath);
    if (OldFileBackendLoader.isControlTag(backendStateAsJsonString.split("\\r?\\n")[0])) {
      return new OldFileBackendLoader().load(stateFilePath.toFile());
    }
    return (BackendState) JSON.toObject(backendStateAsJsonString, BackendState.class);
  }

  private void writeText(String text) throws IOException {
    try {
      writer.write(text);
    } catch (IOException e) {
      LOGGER.error(e);
      throw e;
    }
  }

  @Override
  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }
}
