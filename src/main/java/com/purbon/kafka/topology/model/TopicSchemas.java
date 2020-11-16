package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private Optional<String> keySchemaFile;
  private Optional<String> valueSchemaFile;

  public TopicSchemas() {
    this(Optional.empty(), Optional.empty());
  }

  public TopicSchemas(Optional<JsonNode> keyJsonNode, Optional<JsonNode> valueJsonNode) {
    this.keySchemaFile = Optional.empty();
    this.valueSchemaFile = Optional.empty();
    keyJsonNode.ifPresent(node -> keySchemaFile = Optional.of(node.asText()));
    valueJsonNode.ifPresent(node -> valueSchemaFile = Optional.of(node.asText()));
  }

  public TopicSchemas(String keySchemaFile, String valueSchemaFile) {
    this.keySchemaFile = Optional.of(keySchemaFile);
    this.valueSchemaFile = Optional.of(valueSchemaFile);
  }

  public Optional<String> getKeySchemaFile() {
    return keySchemaFile;
  }

  public Optional<String> getValueSchemaFile() {
    return valueSchemaFile;
  }
}
