package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.validateRequiresKeys;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Maps;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.TopicSchemas;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jersey.repackaged.com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicCustomDeserializer extends StdDeserializer<TopicImpl> {

  private static final Logger LOGGER = LogManager.getLogger(TopicCustomDeserializer.class);

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
    validateRequiresKeys(rootNode, "name");

    String name = rootNode.get("name").asText();

    Optional<JsonNode> optionalDataTypeNode = Optional.ofNullable(rootNode.get("dataType"));
    Optional<String> optionalDataType = optionalDataTypeNode.map(o -> o.asText());

    Optional<JsonNode> optionalConfigNode = Optional.ofNullable(rootNode.get("config"));
    Map<String, String> config =
        optionalConfigNode
            .map(
                node ->
                    Maps.asMap(Sets.newHashSet(node.fieldNames()), (key) -> node.get(key).asText()))
            .orElse(new HashMap<>());

    TopicImpl topic = new TopicImpl(name, optionalDataType, config, this.config);

    Optional.ofNullable(rootNode.get("schemas"))
        .ifPresent(
            node -> {
              topic.setSchemas(
                  new TopicSchemas(
                      node.get("key.schema.file").asText(),
                      node.get("value.schema.file").asText()));
            });
    LOGGER.debug(String.format("Topic %s with config %s has been created", topic, config));
    return topic;
  }
}
