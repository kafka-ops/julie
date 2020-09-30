package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.addTopics2Project;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.Kafka;
import com.purbon.kafka.topology.model.users.platform.KafkaConnect;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  private static final String PROJECTS_KEY = "projects";
  private static final String CONTEXT_KEY = "context";

  private static final String PLATFORM_KEY = "platform";
  private static final String KAFKA_KEY = "kafka";
  private static final String KAFKA_CONNECT_KEY = "kafka_connect";
  private static final String SCHEMA_REGISTRY_KEY = "schema_registry";
  private static final String CONTROL_CENTER_KEY = "control_center";

  private static final String NAME_KEY = "name";
  private static final String CONSUMERS_KEY = "consumers";
  private static final String PRODUCERS_KEY = "producers";
  private static final String CONNECTORS_KEY = "connectors";
  private static final String STREAMS_KEY = "streams";
  private static final String SCHEMAS_KEY = "schemas";
  private static final String RBAC_KEY = "rbac";
  private static final String TOPICS_KEY = "topics";
  private static final String PRINCIPAL_KEY = "principal";

  private final TopologyBuilderConfig config;

  TopologyCustomDeserializer(TopologyBuilderConfig config) {
    this(null, config);
  }

  private TopologyCustomDeserializer(Class<?> clazz, TopologyBuilderConfig config) {
    super(clazz);
    this.config = config;
  }

  @Override
  public Topology deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);

    validateRequiresKeys(rootNode);

    Topology topology = new TopologyImpl(config);
    List<String> excludeAttributes = Arrays.asList(PROJECTS_KEY, CONTEXT_KEY, PLATFORM_KEY);

    Iterator<String> fieldNames = rootNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!excludeAttributes.contains(fieldName)) {
        topology.addOther(fieldName, rootNode.get(fieldName).asText());
      }
    }
    topology.setContext(rootNode.get(CONTEXT_KEY).asText());

    JsonNode platformNode = rootNode.get(PLATFORM_KEY);
    Platform platform = new Platform();
    if (platformNode != null && platformNode.size() > 0) {
      JsonNode kafkaNode = platformNode.get(KAFKA_KEY);
      if (kafkaNode != null) {
        Kafka kafka = parser.getCodec().treeToValue(kafkaNode, Kafka.class);
        platform.setKafka(kafka);
      }
      JsonNode kafkaConnectNode = platformNode.get(KAFKA_CONNECT_KEY);
      if (kafkaConnectNode != null) {
        KafkaConnect kafkaConnect =
            parser.getCodec().treeToValue(kafkaConnectNode, KafkaConnect.class);
        platform.setKafkaConnect(kafkaConnect);
      }
      JsonNode schemaRegistryNode = platformNode.get(SCHEMA_REGISTRY_KEY);
      if (schemaRegistryNode != null) {
        SchemaRegistry schemaRegistry =
            parser.getCodec().treeToValue(schemaRegistryNode, SchemaRegistry.class);
        platform.setSchemaRegistry(schemaRegistry);
      }
      JsonNode controlCenterNode = platformNode.get(CONTROL_CENTER_KEY);
      if (controlCenterNode != null) {
        ControlCenter controlCenter =
            parser.getCodec().treeToValue(controlCenterNode, ControlCenter.class);
        platform.setControlCenter(controlCenter);
      }
    }
    topology.setPlatform(platform);

    JsonNode projects = rootNode.get(PROJECTS_KEY);

    parseProjects(parser, projects, topology, config)
        .forEach(
            project -> {
              topology.addProject(project);
            });

    return topology;
  }

  private List<Project> parseProjects(
      JsonParser parser, JsonNode projectsNode, Topology topology, TopologyBuilderConfig config)
      throws IOException {
    List<Project> projects = new ArrayList<>();
    for (int i = 0; i < projectsNode.size(); i++) {
      Project project = parseProject(parser, projectsNode.get(i), topology, config);
      projects.add(project);
    }
    return projects;
  }

  private Project parseProject(
      JsonParser parser, JsonNode rootNode, Topology topology, TopologyBuilderConfig config)
      throws IOException {

    String nameFieldValue = rootNode.get(NAME_KEY).asText();
    ProjectImpl project = new ProjectImpl(nameFieldValue, config);
    project.setPrefixContextAndOrder(topology.asFullContext(), topology.getOrder());

    JsonSerdesUtils<Consumer> consumerSerdes = new JsonSerdesUtils<>();
    JsonNode consumers = rootNode.get(CONSUMERS_KEY);
    if (consumers != null) {
      List<Consumer> consumersList =
          consumerSerdes.parseApplicationUser(parser, consumers, Consumer.class);
      project.setConsumers(consumersList);
    }

    JsonSerdesUtils<Producer> producerSerdes = new JsonSerdesUtils<>();
    JsonNode producers = rootNode.get(PRODUCERS_KEY);
    if (producers != null) {
      List<Producer> producersList =
          producerSerdes.parseApplicationUser(parser, producers, Producer.class);
      project.setProducers(producersList);
    }

    JsonSerdesUtils<Connector> connectorSerdes = new JsonSerdesUtils<>();
    JsonNode connectors = rootNode.get(CONNECTORS_KEY);
    if (connectors != null) {
      List<Connector> connectorList =
          connectorSerdes.parseApplicationUser(parser, connectors, Connector.class);
      project.setConnectors(connectorList);
    }

    JsonSerdesUtils<KStream> streamsSerdes = new JsonSerdesUtils<>();
    JsonNode streams = rootNode.get(STREAMS_KEY);
    if (streams != null) {
      List<KStream> streamsList =
          streamsSerdes.parseApplicationUser(parser, streams, KStream.class);
      project.setStreams(streamsList);
    }

    JsonSerdesUtils<Schemas> schemasSerdes = new JsonSerdesUtils<>();
    JsonNode schemas = rootNode.get(SCHEMAS_KEY);
    if (schemas != null) {
      List<Schemas> schemasList =
          schemasSerdes.parseApplicationUser(parser, schemas, Schemas.class);
      project.setSchemas(schemasList);
    }

    // Parser optional RBAC object, only there if using RBAC provider
    JsonNode rbacRootNode = rootNode.get(RBAC_KEY);
    if (rbacRootNode != null) {
      Map<String, List<String>> roles = parseOptionalRbacRoles(rbacRootNode);
      project.setRbacRawRoles(roles);
    }

    JsonNode topics = rootNode.get(TOPICS_KEY);
    addTopics2Project(parser, project, topics, config);

    return project;
  }

  private Map<String, List<String>> parseOptionalRbacRoles(JsonNode rbacRootNode) {
    Map<String, List<String>> roles = new HashMap<>();
    for (int i = 0; i < rbacRootNode.size(); i++) {
      JsonNode elem = rbacRootNode.get(i);
      Iterator<String> fields = elem.fieldNames();
      while (fields.hasNext()) {
        String field = fields.next(); // field == RoleName
        List<String> principalsByRole = new ArrayList<>();
        JsonNode principals = elem.get(field);
        for (int j = 0; j < principals.size(); j++) {
          JsonNode principalNode = principals.get(j);
          String principal = principalNode.get(PRINCIPAL_KEY).asText();
          principalsByRole.add(principal);
        }
        roles.put(field, principalsByRole);
      }
    }
    return roles;
  }

  private void validateRequiresKeys(JsonNode rootNode) throws IOException {
    List<String> keys = Arrays.asList(CONTEXT_KEY, PROJECTS_KEY);
    for (String key : keys) {
      if (rootNode.get(key) == null) {
        throw new IOException(key + " is a required field in the topology, please specify.");
      }
    }
  }
}
