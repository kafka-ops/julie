package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.Constants.GCP_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.Constants.KAFKA_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.Constants.REDIS_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.Constants.S3_STATE_PROCESSOR_CLASS;
import static com.purbon.kafka.topology.Constants.STATE_PROCESSOR_DEFAULT_CLASS;

import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.audit.Appender;
import com.purbon.kafka.topology.audit.Auditor;
import com.purbon.kafka.topology.backend.Backend;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.backend.GCPBackend;
import com.purbon.kafka.topology.backend.KafkaBackend;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.backend.S3Backend;
import com.purbon.kafka.topology.utils.Pair;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JulieOpsAuxiliary {

  private static final Logger LOGGER = LogManager.getLogger(JulieOpsAuxiliary.class);

  public static BackendController buildBackendController(Configuration config) throws IOException {

    String backendClass = config.getStateProcessorImplementationClassName();
    Backend backend;
    try {
      if (backendClass.equalsIgnoreCase(STATE_PROCESSOR_DEFAULT_CLASS)) {
        backend = new FileBackend();
      } else if (backendClass.equalsIgnoreCase(REDIS_STATE_PROCESSOR_CLASS)) {
        backend = new RedisBackend(config);
      } else if (backendClass.equalsIgnoreCase(S3_STATE_PROCESSOR_CLASS)) {
        backend = new S3Backend();
      } else if (backendClass.equalsIgnoreCase(GCP_STATE_PROCESSOR_CLASS)) {
        backend = new GCPBackend();
      } else if (backendClass.equalsIgnoreCase(KAFKA_STATE_PROCESSOR_CLASS)) {
        backend = new KafkaBackend();
      } else {
        throw new IOException(backendClass + " Unknown state processor provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    backend.configure(config);
    return new BackendController(backend);
  }

  public static Auditor configureAndBuildAuditor(Configuration config) throws IOException {
    String appenderClassString = config.getJulieAuditAppenderClass();

    try {
      Class anAppenderClass = Class.forName(appenderClassString);
      Appender appender;
      try {
        Constructor constructor = anAppenderClass.getConstructor(Configuration.class);
        appender = (Appender) constructor.newInstance(config);
      } catch (NoSuchMethodException e) {
        LOGGER.trace(
            appenderClassString + " has no config constructor, falling back to a default one");
        Constructor constructor = anAppenderClass.getConstructor();
        appender = (Appender) constructor.newInstance();
      }
      return new Auditor(appender);
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new IOException(e);
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new IOException(e);
    }
  }

  public static KafkaConnectArtefactManager configureKConnectArtefactManager(
      Configuration config, String topologyFileOrDir) {
    Map<String, KConnectApiClient> clients =
        config.getKafkaConnectServers().entrySet().stream()
            .map(
                entry ->
                    new Pair<>(
                        entry.getKey(),
                        new KConnectApiClient(entry.getValue(), entry.getKey(), config)))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    if (clients.isEmpty()) {
      LOGGER.debug(
          "No KafkaConnect clients configured for JulieOps to use, please verify your config file");
    }

    return new KafkaConnectArtefactManager(clients, config, topologyFileOrDir);
  }

  public static KSqlArtefactManager configureKSqlArtefactManager(
      Configuration config, String topologyFileOrDir) {

    Map<String, KsqlApiClient> clients = new HashMap<>();
    if (config.hasKSQLServer()) {
      KsqlApiClient client = new KsqlApiClient(config.getKSQLClientConfig());
      clients.put("default", client);
    }

    if (clients.isEmpty()) {
      LOGGER.debug(
          "No KSQL clients configured for JulieOps to use, please verify your config file");
    }

    return new KSqlArtefactManager(clients, config, topologyFileOrDir);
  }
}
