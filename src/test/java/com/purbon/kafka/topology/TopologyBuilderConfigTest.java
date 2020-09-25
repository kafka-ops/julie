package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
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
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class TopologyBuilderConfigTest {

  Map<String, String> cliOps;
  Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  public void testWithAllRequiredFields() throws ConfigurationException {
    Topology topology = new TopologyImpl();

    props.put(ACCESS_CONTROL_IMPLEMENTATION_CLASS, TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS);
    props.put(MDS_SERVER, "example.com");
    props.put(MDS_USER_CONFIG, "foo");
    props.put(MDS_PASSWORD_CONFIG, "bar");
    props.put(MDS_KAFKA_CLUSTER_ID_CONFIG, "1234");

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

    props.put(TopologyBuilderConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

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

    props.put(TopologyBuilderConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://foo:8082");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);
    config.validateWith(topology);
  }
}
