package com.purbon.kafka.topology.model.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

public class Subject {

  private Optional<String> schemaFile;
  private Optional<String> recordType;

  public Subject(Optional<JsonNode> schemaFileJsonNode, Optional<JsonNode> recordTypeJsonNode) {
    schemaFile = schemaFileJsonNode.map(JsonNode::asText);
    recordType = recordTypeJsonNode.map(JsonNode::asText);
  }

  public Subject(String schemaFile, String recordType) {
    this.schemaFile = Optional.ofNullable(schemaFile);
    this.recordType = Optional.ofNullable(recordType);
  }

  public Optional<String> getSchemaFile() {
    return schemaFile;
  }

  public Optional<String> getRecordType() {
    return recordType;
  }
}
