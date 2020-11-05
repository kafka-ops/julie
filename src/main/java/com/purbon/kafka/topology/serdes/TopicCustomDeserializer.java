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
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    List<Consumer> consumers = getUsers(parser, rootNode, "consumers", Consumer.class);
    List<Producer> producers = getUsers(parser, rootNode, "producers", Producer.class);

    Optional<JsonNode> optionalDataTypeNode = Optional.ofNullable(rootNode.get("dataType"));
    Optional<String> optionalDataType = optionalDataTypeNode.map(o -> o.asText());

    Optional<JsonNode> optionalConfigNode = Optional.ofNullable(rootNode.get("config"));
    Map<String, String> config =
        optionalConfigNode
            .map(
                node ->
                    Maps.asMap(Sets.newHashSet(node.fieldNames()), (key) -> node.get(key).asText()))
            .orElse(new HashMap<>());

    TopicImpl topic = new TopicImpl(name, producers, consumers, optionalDataType, config, this.config);

    Optional.ofNullable(rootNode.get("schemas"))
        .ifPresent(
            node -> {
              topic.setSchemas(
                  new TopicSchemas(
                      node.get("key.schema.file").asText(),
                      node.get("value.schema.file").asText()));
            });
    LOGGER.debug(
        String.format("Topic %s with config %s has been created", topic.getName(), config));
    return topic;
  }

  private <T extends User> List<T> getUsers(JsonParser parser, JsonNode rootNode, String fieldName, Class<T> tClass) throws com.fasterxml.jackson.core.JsonProcessingException {
    JsonNode jsonNode = rootNode.get(fieldName);
    return jsonNode == null ? new ArrayList<>() : new JsonSerdesUtils<T>().parseApplicationUser(parser, jsonNode, tClass);
  }
}
