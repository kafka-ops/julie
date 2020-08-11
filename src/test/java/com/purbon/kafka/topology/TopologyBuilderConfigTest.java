package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.KafkaTopologyBuilder.SCHEMA_REGISTRY_URL;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;

import com.purbon.kafka.topology.exceptions.ConfigurationException;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.Topology;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Before;
import org.junit.Test;

public class TopologyBuilderConfigTest {

  Map<String, String> cliOps;
  Configuration props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new MapConfiguration(new HashMap<>());
  }

  @Test
  public void testWithAllRequiredFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();

    props.addProperty(
        ACCESS_CONTROL_IMPLEMENTATION_CLASS, TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS);
    props.addProperty(MDS_SERVER, "example.com");
    props.addProperty(MDS_USER_CONFIG, "foo");
    props.addProperty(MDS_PASSWORD_CONFIG, "bar");
    props.addProperty(MDS_KAFKA_CLUSTER_ID_CONFIG, "1234");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test(expected = ConfigurationException.class)
  public void testSchemaRegistryConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    TopicSchemas schema = new TopicSchemas();
    schema.setKeySchemaFile("foo");
    schema.setValueSchemaFile("bar");
    topic.setSchemas(schema);
    project.addTopic(topic);
    topology.addProject(project);

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testSchemaRegistryValidConfigFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    TopicSchemas schema = new TopicSchemas();
    schema.setKeySchemaFile("foo");
    schema.setValueSchemaFile("bar");
    topic.setSchemas(schema);
    project.addTopic(topic);
    topology.addProject(project);

    props.addProperty(SCHEMA_REGISTRY_URL, "http://foo:8082");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }

  @Test
  public void testSchemaRegistryValidConfigButNoSchemas() throws ConfigurationException {
    Topology topology = new TopologyImpl();
    Project project = new ProjectImpl();
    Topic topic = new TopicImpl();
    project.addTopic(topic);
    topology.addProject(project);

    props.addProperty(SCHEMA_REGISTRY_URL, "http://foo:8082");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }
}
