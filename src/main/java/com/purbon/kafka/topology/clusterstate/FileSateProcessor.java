package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSateProcessor implements StateProcessor {

  private static final Logger LOGGER = LogManager.getLogger(FileSateProcessor.class);

  private Writer writer;
  private String expression = "^\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\'";
  private Pattern regexp;

  public FileSateProcessor() {
    this.regexp = Pattern.compile(expression);
    this.writer = null;
  }

  @Override
  public void createOrOpen() {
    try {
      writer = new FileWriter(filename());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<TopologyAclBinding> load() throws IOException {
    File file = new File(filename());
    return load(file.toURI());
  }

  public List<TopologyAclBinding> load(URI uri) throws IOException {

    Path filePath = Paths.get(uri);
    List<TopologyAclBinding> bindings = new ArrayList<>();
    BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()));
    String type = in.readLine();
    String line = null;
    while ( (line = in.readLine()) != null ) {
      TopologyAclBinding binding = null;
      if (type.equalsIgnoreCase("acls")) {
        binding = buildAclBinding(line);
      } else if (type.equalsIgnoreCase("rbac")) {
        binding = buildRBACBinding(line);
      } else {
        throw new IOException("Binding type ( "+type+" )not supported.");
      }
      bindings.add(binding);
    }
    return bindings;
  }

  private TopologyAclBinding buildRBACBinding(String line) {
    return null;
  }

  private TopologyAclBinding buildAclBinding(String line) throws IOException {
    //'GROUP', 'null', '*', 'READ', 'User:Connect1', 'null', '*', 'LITERAL'
    Matcher matches = regexp.matcher(line);

    if (matches.groupCount() != 8 || !matches.matches()) {
      throw new IOException(("line does not match"));
    }

    return TopologyAclBinding.build(
        matches.group(1),
        matches.group(2),
        matches.group(3),
        matches.group(4),
        matches.group(5),
        matches.group(6),
        matches.group(7),
        matches.group(8)
    );
  }

  public void saveType(String type) {
    try {
      writer.write(type);
      writer.write("\n");
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void saveBindings(List<TopologyAclBinding> bindings) {
    bindings.forEach(binding -> {
      try {
        writer.write(binding.toString());
        writer.write("\n");
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

  private String filename() {
    return ".cluster-state";
  }}
