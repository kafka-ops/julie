package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.addProject2Topology;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Platform;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.Kafka;
import com.purbon.kafka.topology.model.users.platform.KafkaConnect;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  public static final String PROJECTS_KEY = "projects";
  public static final String CONTEXT_KEY = "context";

  public static final String PLATFORM_KEY = "platform";
  public static final String KAFKA_KEY = "kafka";
  public static final String KAFKA_CONNECT_KEY = "kafka_connect";
  public static final String SCHEMA_REGISTRY_KEY = "schema_registry";
  public static final String CONTROL_CENTER_KEY = "control_center";

  protected TopologyCustomDeserializer() {
    this(null);
  }

  protected TopologyCustomDeserializer(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public Topology deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);

    validateRequiresKeys(rootNode);

    JsonNode projects = rootNode.get(PROJECTS_KEY);
    Topology topology = new TopologyImpl();

    addProject2Topology(parser, topology, projects);

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

    return topology;
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
