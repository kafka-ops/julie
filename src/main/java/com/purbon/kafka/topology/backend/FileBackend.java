package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileBackend implements Backend {

  private static final Logger LOGGER = LogManager.getLogger(FileBackend.class);
  public static final String STATE_FILE_NAME = ".cluster-state";

  private RandomAccessFile writer;
  private String expression =
      "^\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\'";
  private Pattern regexp;

  public FileBackend() {
    this.regexp = Pattern.compile(expression);
    this.writer = null;
  }

  @Override
  public void createOrOpen() {
    createOrOpen(Mode.APPEND);
  }

  @Override
  public void createOrOpen(Mode mode) {
    try {
      writer = new RandomAccessFile(STATE_FILE_NAME, "rw");
      Path path = Paths.get(STATE_FILE_NAME);
      if (path.toFile().exists()) {
        writer.seek(0);
        if (mode.equals(Mode.TRUNCATE)) {
          writer.getChannel().truncate(0);
        }
      }
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
    BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()));
    String type = in.readLine();
    String line = null;
    while ((line = in.readLine()) != null) {
      TopologyAclBinding binding = null;
      if (line.equalsIgnoreCase("ServiceAccounts")) {
        // process service accounts, should break from here.
        break;
      }
      if (type.equalsIgnoreCase("acls")) {
        binding = buildAclBinding(line);
      } else {
        throw new IOException("Binding type ( " + type + " )not supported.");
      }
      bindings.add(binding);
    }
    return bindings;
  }

  public Set<ServiceAccount> loadServiceAccounts() throws IOException {
    if (writer == null) {
      throw new IOException("state file does not exist");
    }
    Path filePath = Paths.get(STATE_FILE_NAME);
    Set<ServiceAccount> accounts = new HashSet<>();
    BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()));
    String line = null;
    while ((line = in.readLine()) != null) {
      if (line.equalsIgnoreCase("ServiceAccounts")) {
        // process service accounts, should break from here.
        break;
      }
    }
    if (line != null && line.equalsIgnoreCase("ServiceAccounts")) {
      while ((line = in.readLine()) != null) {
        ServiceAccount account = (ServiceAccount) JSON.toObject(line, ServiceAccount.class);
        accounts.add(account);
      }
    }
    return accounts;
  }

  private TopologyAclBinding buildRBACBinding(String line) {
    return null;
  }

  private TopologyAclBinding buildAclBinding(String line) throws IOException {
    // 'TOPIC', 'topicB', '*', 'READ', 'User:Connect1', 'LITERAL'
    Matcher matches = regexp.matcher(line);

    if (matches.groupCount() != 6 || !matches.matches()) {
      throw new IOException(("line (" + line + ") does not match"));
    }

    return TopologyAclBinding.build(
        matches.group(1), // resourceType
        matches.group(2), // resourceName
        matches.group(3), // host
        matches.group(4), // operation
        matches.group(5), // principal
        matches.group(6) // pattern
        );
  }

  public void saveType(String type) {
    try {
      writer.writeBytes(type);
      writer.writeBytes("\n");
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void saveBindings(Set<TopologyAclBinding> bindings) {
    bindings.stream()
        .sorted()
        .forEach(
            binding -> {
              try {
                writer.writeBytes(binding.toString());
                writer.writeBytes("\n");
              } catch (IOException e) {
                LOGGER.error(e);
              }
            });
  }

  @Override
  public void saveAccounts(Set<ServiceAccount> accounts) {
    accounts.forEach(
        account -> {
          try {
            writer.writeBytes(JSON.asString(account));
            writer.writeBytes("\n");
          } catch (IOException e) {
            LOGGER.error(e);
          }
        });
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
