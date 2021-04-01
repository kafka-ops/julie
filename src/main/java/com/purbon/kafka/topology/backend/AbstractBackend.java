package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractBackend implements Backend {

  protected static final String TOPIC_FILE_NAME = "julie-topics";
  protected static final String SA_FILE_NAME = "julie-accounts";
  protected static final String BINDINGS_FILE_NAME = "julie-bindings";

  private final String expression =
      "^\"?\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(.+)\\',\\s*\\'(\\S+)\\'\"?$";
  private Pattern regexp;

  public AbstractBackend() {
    this.regexp = Pattern.compile(expression);
  }

  protected TopologyAclBinding buildAclBinding(String line) throws IOException {
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
}
