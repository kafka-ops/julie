package com.purbon.kafka.topology.model.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.purbon.kafka.topology.model.Topic;
import java.io.IOException;
import java.util.Optional;

public class Subject {

  private Optional<String> schemaFile;
  private Optional<String> recordType;
  private SubjectKind kind;

  public enum SubjectKind {
    KEY("key"),
    VALUE("value");

    private final String label;

    SubjectKind(String label) {
      this.label = label;
    }
  }

  public Subject(
      Optional<JsonNode> schemaFileJsonNode,
      Optional<JsonNode> recordTypeJsonNode,
      SubjectKind kind) {
    this.schemaFile = schemaFileJsonNode.map(JsonNode::asText);
    this.recordType = recordTypeJsonNode.map(JsonNode::asText);
    this.kind = kind;
  }

  public Subject(String schemaFile, String recordType, SubjectKind kind) {
    this.schemaFile = Optional.ofNullable(schemaFile);
    this.recordType = Optional.ofNullable(recordType);
    this.kind = kind;
  }

  public Optional<String> getSchemaFile() {
    return schemaFile;
  }

  private String recordTypeAsString() throws IOException {
    return recordType.orElseThrow(() -> new IOException("Missing record type for " + schemaFile));
  }

  public String buildSubjectName(Topic topic) throws IOException {
    switch (topic.getSubjectNameStrategy()) {
      case TOPIC_NAME_STRATEGY:
        return topic.toString() + "-" + kind.label;
      case RECORD_NAME_STRATEGY:
        return recordTypeAsString();
      case TOPIC_RECORD_NAME_STRATEGY:
        return topic.toString() + "-" + recordTypeAsString();
      default:
        return "";
    }
  }
}
