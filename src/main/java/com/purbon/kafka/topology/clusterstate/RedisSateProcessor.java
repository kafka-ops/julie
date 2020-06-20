package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisSateProcessor implements StateProcessor {

  private static final Logger LOGGER = LogManager.getLogger(RedisSateProcessor.class);
  private static final String KAFKA_TOPOLOGY_BUILDER_BINDINGS = "kafka.topology.builder.bindings";
  private static final String KAFKA_TOPOLOGY_BUILDER_TYPE = "kafka.topology.builder.type";

  private String expression =
      "^\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\'";
  private Pattern regexp;
  private Jedis jedis;

  public RedisSateProcessor(String host, int port) {
    this.jedis = new Jedis(host, port);
    this.regexp = Pattern.compile(expression);
  }

  @Override
  public void createOrOpen() {
    jedis.connect();
  }

  @Override
  public List<TopologyAclBinding> load() throws IOException {
    return load(null);
  }

  @Override
  public List<TopologyAclBinding> load(URI uri) throws IOException {
    List<TopologyAclBinding> bindings = new ArrayList<>();
    String type = jedis.get(KAFKA_TOPOLOGY_BUILDER_TYPE);

    long count = jedis.scard(KAFKA_TOPOLOGY_BUILDER_BINDINGS);
    for (long i = 0; i < count; i++) {
      String elem = jedis.spop(KAFKA_TOPOLOGY_BUILDER_BINDINGS);
      TopologyAclBinding binding = buildAclBinding(elem);
      bindings.add(binding);
    }

    return bindings;
  }

  @Override
  public void saveType(String type) {
    jedis.set(KAFKA_TOPOLOGY_BUILDER_TYPE, type);
  }

  @Override
  public void saveBindings(List<TopologyAclBinding> bindings) {

    String[] members =
        bindings.stream().map(binding -> binding.toString()).toArray(size -> new String[size]);

    jedis.sadd(KAFKA_TOPOLOGY_BUILDER_BINDINGS, members);
  }

  @Override
  public void close() {
    jedis.close();
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
}
