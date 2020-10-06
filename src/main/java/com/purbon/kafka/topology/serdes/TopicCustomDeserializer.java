package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.TopicSchemas;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

public class TopicCustomDeserializer extends StdDeserializer<TopicImpl> {

  private final TopologyBuilderConfig config;

  TopicCustomDeserializer(TopologyBuilderConfig config) {
    this(null, config);
  }

  private TopicCustomDeserializer(Class<?> clazz, TopologyBuilderConfig config) {
    super(clazz);
    this.config = config;
  }

  @Override
  public TopicImpl deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode rootNode = parser.getCodec().readTree(parser);
    String name = rootNode.get("name").asText();
    JsonNode dataTypeNode = rootNode.get("dataType");
    Optional<String> optionalDataType =
        dataTypeNode == null ? Optional.empty() : Optional.of(dataTypeNode.asText());

    JsonNode configNode = rootNode.get("config");
    HashMap<String, String> config = new HashMap<>();
    if (configNode != null) {
      configNode
          .fields()
          .forEachRemaining(entry -> config.put(entry.getKey(), entry.getValue().asText()));
    }
    TopicImpl topic = new TopicImpl(name, optionalDataType, config, this.config);

    JsonNode schemasNode = rootNode.get("schemas");
    if (schemasNode != null) {
      TopicSchemas schemas =
          new TopicSchemas(
              schemasNode.get("key.schema.file").asText(),
              schemasNode.get("value.schema.file").asText());
      topic.setSchemas(schemas);
    }
    return topic;
  }
}
