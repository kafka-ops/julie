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
}
