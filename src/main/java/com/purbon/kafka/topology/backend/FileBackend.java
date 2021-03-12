package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileBackend extends AbstractBackend {

  private static final Logger LOGGER = LogManager.getLogger(FileBackend.class);
  public static final String STATE_FILE_NAME = ".cluster-state";
  static final String SERVICE_ACCOUNTS_TAG = "ServiceAccounts";
  static final String TOPICS_TAG = "Topics";
  static final String ACLS_TAG = "acls";

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

  public Set<TopologyAclBinding> loadBindings() throws IOException {
    if (writer == null) {
      throw new IOException("state file does not exist");
    }
    File file = new File(STATE_FILE_NAME);
    return load(file.toURI());
  }

  public Set<TopologyAclBinding> load(URI uri) throws IOException {
    Path filePath = Paths.get(uri);
    Set<TopologyAclBinding> bindings = new LinkedHashSet<>();
    try (BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()))) {
      String type = in.readLine();
      String line = null;
      while ((line = in.readLine()) != null) {
        TopologyAclBinding binding = null;
        if (line.equalsIgnoreCase("ServiceAccounts")) {
          // process service accounts, should break from here.
          break;
        }
        if (type.equalsIgnoreCase(ACLS_TAG)) {
          binding = buildAclBinding(line);
        } else {
          throw new IOException("Binding type ( " + type + " )not supported.");
        }
        bindings.add(binding);
      }
    }
    return bindings;
  }

  public Set<ServiceAccount> loadServiceAccounts() throws IOException {
    return loadItemsFromFile(
            SERVICE_ACCOUNTS_TAG,
            line -> {
              try {
                return JSON.toObject(line, ServiceAccount.class);
              } catch (JsonProcessingException e) {
                LOGGER.error(e);
                return null;
              }
            })
        .stream()
        .filter(Objects::nonNull)
        .map(o -> (ServiceAccount) o)
        .collect(Collectors.toSet());
  }

  private boolean foundAControlTag(String line) {
    return line.equalsIgnoreCase(SERVICE_ACCOUNTS_TAG)
        || line.equalsIgnoreCase(TOPICS_TAG)
        || line.equalsIgnoreCase(ACLS_TAG);
  }

  @Override
  public Set<String> loadTopics() throws IOException {
    return loadItemsFromFile(TOPICS_TAG, String::trim).stream()
        .map(String::valueOf)
        .collect(Collectors.toSet());
  }

  private Set<Object> loadItemsFromFile(String tag, Function<String, Object> buildFunction)
      throws IOException {
    if (writer == null) {
      throw new IOException("state file does not exist");
    }
    Set<Object> elements = new HashSet<>();
    try (BufferedReader in = openLocalStateFile()) {
      String line = moveFileToTag(tag, in);
      if (line != null && line.equalsIgnoreCase(tag)) {
        while ((line = in.readLine()) != null && !foundAControlTag(line)) {
          elements.add(buildFunction.apply(line.trim()));
        }
      }
    }
    return elements;
  }

  private BufferedReader openLocalStateFile() throws IOException {
    Path filePath = Paths.get(STATE_FILE_NAME);
    return new BufferedReader(new FileReader(filePath.toFile()));
  }

  private String moveFileToTag(String tag, BufferedReader in) throws IOException {
    String line = null;
    while ((line = in.readLine()) != null) {
      if (line.equalsIgnoreCase(tag)) {
        break; // process elements, should start from here.
      }
    }
    return line;
  }

  public void saveType(String type) {
    writeLine(type);
  }

  @Override
  public void saveBindings(Set<TopologyAclBinding> bindings) {
    bindings.stream().sorted().forEach(b -> writeLine(b.toString()));
  }

  @Override
  public void saveAccounts(Set<ServiceAccount> accounts) {
    accounts.forEach(
        a -> {
          try {
            writeLine(JSON.asString(a));
          } catch (JsonProcessingException e) {
            LOGGER.error(e);
          }
        });
  }

  @Override
  public void saveTopics(Set<String> topics) {
    topics.forEach(this::writeLine);
  }

  private void writeLine(String line) {
    try {
      writer.write(line);
      writer.write("\n");
    } catch (IOException e) {
      LOGGER.error(e);
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
