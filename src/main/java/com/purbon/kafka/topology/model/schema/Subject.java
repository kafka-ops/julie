package com.purbon.kafka.topology.model.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.purbon.kafka.topology.model.Topic;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import java.io.IOException;
import java.util.Optional;

public class Subject {

  private Optional<String> schemaFile;
  private Optional<String> recordType;
  private Optional<String> optionalCompatibility;
  private Optional<String> optionalFormat;
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
      Optional<JsonNode> optionalFormat,
      Optional<JsonNode> optionalCompatibility,
      SubjectKind kind) {
    this.schemaFile = schemaFileJsonNode.map(JsonNode::asText);
    this.recordType = recordTypeJsonNode.map(JsonNode::asText);
    this.optionalCompatibility = optionalCompatibility.map(JsonNode::asText);
    this.optionalFormat = optionalFormat.map(JsonNode::asText);
    this.kind = kind;
  }

  public Subject(String schemaFile, String recordType, SubjectKind kind) {
    this.schemaFile = Optional.ofNullable(schemaFile);
    this.recordType = Optional.ofNullable(recordType);
    this.kind = kind;
  }

  public String getSchemaFile() throws IOException {
    return schemaFile.orElseThrow(() -> new IOException("SchemaFile not present"));
  }

  public boolean hasSchemaFile() {
    return schemaFile.isPresent();
  }

  private String recordTypeAsString() throws IOException {
    return recordType.orElseThrow(() -> new IOException("Missing record type for " + schemaFile));
  }

  public String getFormat() {
    return optionalFormat.orElse(AvroSchema.TYPE);
  }

  public Optional<String> getOptionalCompatibility() {
    return optionalCompatibility;
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
