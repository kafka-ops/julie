package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.utils.JSON;
import com.purbon.kafka.topology.utils.ZKClient;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDSBaseTest {

  private static final Logger LOGGER = LogManager.getLogger(MDSBaseTest.class);
  private ZKClient zkClient;

  public void beforeEach() throws IOException, InterruptedException {
    zkClient = new ZKClient();
    zkClient.connect("localhost");
  }

  protected String getKafkaClusterID() {
    /* TODO: This method is the only reason JulieOps depends on zookeeper,
     * a component that is about to be retired. Figure out a more modern way
     * to get the custer id, and remove the zookeeper stuff from the code. */
    try {
      String nodeData = zkClient.getNodeData("/cluster/id");
      return JSON.toMap(nodeData).get("id").toString();
    } catch (IOException e) {
      LOGGER.error(e);
    }
    return "-1";
  }

  protected String getSchemaRegistryClusterID() {
    return "schema-registry";
  }

  protected String getKafkaConnectClusterID() {
    return "connect-cluster";
  }

  protected String getKSqlClusterID() {
    return "ksqldb";
  }
}
