package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisBackend extends AbstractBackend {

  private static final Logger LOGGER = LogManager.getLogger(RedisBackend.class);

  static final String JULIE_OPS_BINDINGS = "julie.ops.bindings";
  static final String JULIE_OPS_TYPE = "julie.ops.type";

  private Jedis jedis;

  public RedisBackend(String host, int port) {
    this(new Jedis(host, port));
  }

  public RedisBackend(Jedis jedis) {
    super();
    this.jedis = jedis;
  }

  @Override
  public void createOrOpen() {
    createOrOpen(Mode.APPEND);
  }

  @Override
  public void createOrOpen(Mode mode) {
    jedis.connect();
    if (mode.equals(Mode.TRUNCATE)) {
      jedis.del(JULIE_OPS_TYPE);
      jedis.del(JULIE_OPS_BINDINGS);
    }
  }

  @Override
  public Set<TopologyAclBinding> loadBindings() throws IOException {
    connectIfNeed();

    Set<TopologyAclBinding> bindings = new HashSet<>();
    String type = jedis.get(JULIE_OPS_TYPE);

    long count = jedis.scard(JULIE_OPS_BINDINGS);
    for (long i = 0; i < count; i++) {
      String elem = jedis.spop(JULIE_OPS_BINDINGS);
      TopologyAclBinding binding = buildAclBinding(elem);
      bindings.add(binding);
    }

    return bindings;
  }

  private void connectIfNeed() {
    if (!jedis.isConnected()) {
      createOrOpen();
    }
  }

  @Override
  public Set<ServiceAccount> loadServiceAccounts() throws IOException {
    return new HashSet<>();
  }

  @Override
  public Set<String> loadTopics() throws IOException {
    return new HashSet<>();
  }

  @Override
  public void saveType(String type) {
    jedis.set(JULIE_OPS_TYPE, type);
  }

  @Override
  public void saveBindings(Set<TopologyAclBinding> bindings) {

    String[] members =
        bindings.stream().map(binding -> binding.toString()).toArray(size -> new String[size]);

    jedis.sadd(JULIE_OPS_BINDINGS, members);
  }

  @Override
  public void saveAccounts(Set<ServiceAccount> accounts) {}

  @Override
  public void saveTopics(Set<String> topics) {}

  @Override
  public void close() {
    jedis.close();
  }
}
