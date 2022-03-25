package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController.Mode;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisBackend implements Backend {

  private static final Logger LOGGER = LogManager.getLogger(RedisBackend.class);

  private final String bucket;
  private final Jedis jedis;

  public RedisBackend(String host, int port, String bucket) {
    this(new Jedis(host, port), bucket);
  }

  public RedisBackend(Jedis jedis, String bucket) {
    this.jedis = jedis;
    this.bucket = bucket;
  }

  public RedisBackend(Configuration config) {
    this(config.getRedisHost(), config.getRedisPort(), config.getRedisBucket());
  }

  @Override
  public void createOrOpen() {
    createOrOpen(Mode.APPEND);
  }

  @Override
  public void createOrOpen(Mode mode) {
    jedis.connect();
    if (mode.equals(Mode.TRUNCATE)) {
      jedis.del(bucket);
    }
  }

  @Override
  public void close() {
    jedis.close();
  }

  @Override
  public void save(BackendState state) throws IOException {
    LOGGER.debug("Storing state for: " + state);
    jedis.set(bucket, state.asPrettyJson());
  }

  @Override
  public BackendState load() throws IOException {
    connectIfNeed();
    Optional<String> contentOptional = Optional.ofNullable(jedis.get(bucket));
    LOGGER.debug("Loading a new state instance: " + contentOptional);
    return (BackendState) JSON.toObject(contentOptional.orElse("{}"), BackendState.class);
  }

  private void connectIfNeed() {
    if (!jedis.isConnected()) {
      createOrOpen();
    }
  }
}
