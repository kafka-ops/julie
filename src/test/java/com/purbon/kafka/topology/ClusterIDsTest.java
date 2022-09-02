package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.MDS_VALID_CLUSTER_IDS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.purbon.kafka.topology.api.mds.ClusterIDs;
import com.purbon.kafka.topology.exceptions.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterIDsTest {

  private Map<String, String> cliOps;
  private Properties props;

  @BeforeEach
  void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @AfterEach
  void after() {
  }

  @Test
  void shouldRaiseAnExceptionIfAnInvalidClusterIdIsUsed() {
    assertThrows(
        ValidationException.class,
        () -> {
          props.put(MDS_VALID_CLUSTER_IDS_CONFIG + ".0", "kafka-cluster");
          Configuration config = new Configuration(cliOps, props);

          ClusterIDs ids = new ClusterIDs(Optional.of(config));
          ids.setKafkaClusterId("foo");
        });
  }

  @Test
  void shouldAcceptAValidID() {
    props.put(MDS_VALID_CLUSTER_IDS_CONFIG + ".0", "kafka-cluster");
    Configuration config = new Configuration(cliOps, props);

    ClusterIDs ids = new ClusterIDs(Optional.of(config));
    ids.setKafkaClusterId("kafka-cluster");
  }

  @Test
  void shouldAnyIdIfTheListIsEmpty() {
    Configuration config = new Configuration(cliOps, props);

    ClusterIDs ids = new ClusterIDs(Optional.of(config));
    ids.setKafkaClusterId("kafka-cluster");
    ids.setKafkaClusterId("kafka-cluster       ");
    ids.setKafkaClusterId("");
  }
}
