package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisStateProcessor implements StateProcessor {

  private static final Logger LOGGER = LogManager.getLogger(RedisStateProcessor.class);
  static final String KAFKA_TOPOLOGY_BUILDER_BINDINGS = "kafka.topology.builder.bindings";
  static final String KAFKA_TOPOLOGY_BUILDER_TYPE = "kafka.topology.builder.type";

  private String expression =
      "^\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\',\\s*\\'(\\S+)\\'";
  private Pattern regexp;
  private Jedis jedis;

  public RedisStateProcessor(String host, int port) {
    this(new Jedis(host, port));
  }

  public RedisStateProcessor(Jedis jedis) {
    this.jedis = jedis;
    this.regexp = Pattern.compile(expression);
  }

  @Override
  public void createOrOpen() {
    jedis.connect();
  }

  @Override
  public Set<TopologyAclBinding> load() throws IOException {
    return load(null);
  }

  @Override
  public Set<TopologyAclBinding> load(URI uri) throws IOException {

    if (!jedis.isConnected()) {
      createOrOpen();
    }

    Set<TopologyAclBinding> bindings = new HashSet<>();
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
  public void saveBindings(Set<TopologyAclBinding> bindings) {

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
