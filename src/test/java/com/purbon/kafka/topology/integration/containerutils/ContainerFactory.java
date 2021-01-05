package com.purbon.kafka.topology.integration.containerutils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.utility.DockerImageName;

public class ContainerFactory {

  private static final Logger LOGGER = LogManager.getLogger(ContainerFactory.class);

  public static SaslPlaintextKafkaContainer fetchSaslKafkaContainer(String version) {
    LOGGER.debug("Fetching SASL Kafka Container with version=" + version);
    if (version == null || version.isEmpty()) {
      return new SaslPlaintextKafkaContainer();
    } else {
      DockerImageName containerImage =
          DockerImageName.parse("confluentinc/cp-kafka").withTag(version);
      return new SaslPlaintextKafkaContainer(containerImage);
    }
  }
}
