package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.Constants.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.MDS_VALID_CLUSTER_IDS_CONFIG;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClusterIDsTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @After
  public void after() {}

  @Test(expected = ValidationException.class)
  public void shouldRaiseAnExceptionIfAnInvalidClusterIdIsUsed() {
    props.put(MDS_VALID_CLUSTER_IDS_CONFIG + ".0", "kafka-cluster");
    Configuration config = new Configuration(cliOps, props);

    ClusterIDs ids = new ClusterIDs(Optional.of(config));
    ids.setKafkaClusterId("foo");
  }

  @Test
  public void shouldAcceptAValidID() {
    props.put(MDS_VALID_CLUSTER_IDS_CONFIG + ".0", "kafka-cluster");
    Configuration config = new Configuration(cliOps, props);

    ClusterIDs ids = new ClusterIDs(Optional.of(config));
    ids.setKafkaClusterId("kafka-cluster");
  }

  @Test
  public void shouldAnyIdIfTheListIsEmpty() {
    Configuration config = new Configuration(cliOps, props);

    ClusterIDs ids = new ClusterIDs(Optional.of(config));
    ids.setKafkaClusterId("kafka-cluster");
    ids.setKafkaClusterId("kafka-cluster       ");
    ids.setKafkaClusterId("");
  }
}
