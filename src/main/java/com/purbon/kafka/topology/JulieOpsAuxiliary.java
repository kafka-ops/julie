package com.purbon.kafka.topology;

import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.audit.*;
import com.purbon.kafka.topology.backend.*;
import com.purbon.kafka.topology.utils.Pair;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class JulieOpsAuxiliary {

  private static final Logger LOGGER = LogManager.getLogger(JulieOpsAuxiliary.class);

  public static BackendController buildBackendController(Configuration config) throws IOException {
    String backendClass = config.getStateProcessorImplementationClassName();
    var backend = (Backend) initializeClassFromString(backendClass, config);
    backend.configure(config);
    return new BackendController(backend);
  }

  public static Auditor configureAndBuildAuditor(Configuration config) throws IOException {
    if (!config.isJulieAuditEnabled()) {
      return new VoidAuditor();
    }
    String appenderClassString = config.getJulieAuditAppenderClass();
    var appender = (Appender) initializeClassFromString(appenderClassString, config);
    return new Auditor(appender);
  }

  private static Object initializeClassFromString(String classNameString, Configuration config)
      throws IOException {
    try {
      Class aClass = Class.forName(classNameString);
      Object newObject;
      try {
        Constructor constructor = aClass.getConstructor(Configuration.class);
        newObject = constructor.newInstance(config);
      } catch (NoSuchMethodException e) {
        LOGGER.trace(classNameString + " has no config constructor, falling back to a default one");
        Constructor constructor = aClass.getConstructor();
        newObject = constructor.newInstance();
      }
      return newObject;
    } catch (ClassNotFoundException
        | NoSuchMethodException
        | IllegalAccessException
        | InstantiationException
        | InvocationTargetException e) {
      throw new IOException(e);
    }
  }

  public static KafkaConnectArtefactManager configureKConnectArtefactManager(
      Configuration config, String topologyFileOrDir) throws IOException {
    Map<String, KConnectApiClient> clients = new HashMap<>();
    for (var entry : config.getKafkaConnectServers().entrySet()) {
      var pair =
          new Pair<>(
              entry.getKey(), new KConnectApiClient(entry.getValue(), entry.getKey(), config));
      if (clients.put(pair.getKey(), pair.getValue()) != null) {
        throw new IllegalStateException("Duplicate key");
      }
    }

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

  public static void configureLogsInDebugMode(Configuration config) {
    if (!config.areJulieLogsInDebugMode()) {
      return;
    }
    Configurator.setAllLevels("com.purbon.kafka", Level.DEBUG);
  }
}
