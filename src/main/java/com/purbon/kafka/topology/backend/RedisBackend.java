package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisBackend implements Backend {

  private static final Logger LOGGER = LogManager.getLogger(RedisBackend.class);

  public static final String JULIE_OPS_STATE = "julie.ops.state";

  private Jedis jedis;

  public RedisBackend(String host, int port) {
    this(new Jedis(host, port));
  }

  public RedisBackend(Jedis jedis) {
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
      jedis.del(JULIE_OPS_STATE);
    }
  }

  @Override
  public void close() {
    jedis.close();
  }

  @Override
  public void save(BackendState state) throws IOException {
    LOGGER.debug("Storing state for: " + state);
    jedis.set(JULIE_OPS_STATE, state.asPrettyJson());
  }

  @Override
  public BackendState load() throws IOException {
    connectIfNeed();
    Optional<String> contentOptional = Optional.ofNullable(jedis.get(JULIE_OPS_STATE));
    LOGGER.debug("Loading a new state instance: " + contentOptional);
    return (BackendState) JSON.toObject(contentOptional.orElse("{}"), BackendState.class);
  }

  private void connectIfNeed() {
    if (!jedis.isConnected()) {
      createOrOpen();
    }
  }
}
