package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.validateRequiresKeys;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.TopicSchemas;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicCustomDeserializer extends StdDeserializer<TopicImpl> {

  private static final Logger LOGGER = LogManager.getLogger(TopicCustomDeserializer.class);

  private final TopologyBuilderConfig config;
  private PlanMap plans;

  TopicCustomDeserializer(TopologyBuilderConfig config, PlanMap plans) {
    this(null, config, plans);
  }

  private TopicCustomDeserializer(Class<?> clazz, TopologyBuilderConfig config, PlanMap plans) {
    super(clazz);
    this.config = config;
    this.plans = plans;
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
    Optional<String> optionalDataType = optionalDataTypeNode.map(JsonNode::asText);

    Optional<JsonNode> optionalConfigNode = Optional.ofNullable(rootNode.get("config"));
    Map<String, String> config =
        optionalConfigNode
            .map(
                node -> {
                  Map<String, String> map = new HashMap<>();
                  Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                  while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    map.put(entry.getKey(), entry.getValue().asText());
                  }
                  return map;
                })
            .orElse(new HashMap<>());

    Optional<JsonNode> optionalPlanLabel = Optional.ofNullable(rootNode.get("plan"));
    if (optionalPlanLabel.isPresent() && plans.size() == 0) {
      throw new IOException("A plan definition is required if the topology uses them");
    }
    optionalPlanLabel.ifPresent(
        jsonNode -> {
          String planLabel = jsonNode.asText();
          if (plans.containsKey(planLabel)) {
            Map<String, String> planConfigObject = plans.get(planLabel).getConfig();
            planConfigObject.forEach(config::put);
          } else {
            LOGGER.warn(planLabel + " is missing in the plans definition. It will be ignored.");
          }
        });

    TopicImpl topic =
        new TopicImpl(name, producers, consumers, optionalDataType, config, this.config);

    Optional.ofNullable(rootNode.get("schemas"))
        .ifPresent(
            node -> {
              topic.setSchemas(
                  Optional.of(
                      new TopicSchemas(
                          Optional.ofNullable(node.get("key.schema.file")),
                          Optional.ofNullable(node.get("value.schema.file")))));
            });
    if (topic.getSchemas().isPresent()
        && !topic.getSchemas().get().getValueSchemaFile().isPresent()) {
      throw new IOException(
          String.format(
              "Missing required value.schema.file on schemas for topic %s", topic.getName()));
    }
    LOGGER.debug(
        String.format("Topic %s with config %s has been created", topic.getName(), config));
    return topic;
  }

  private <T extends User> List<T> getUsers(
      JsonParser parser, JsonNode rootNode, String fieldName, Class<T> tClass)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    JsonNode jsonNode = rootNode.get(fieldName);
    return jsonNode == null
        ? new ArrayList<>()
        : new JsonSerdesUtils<T>().parseApplicationUser(parser, jsonNode, tClass);
  }
}
