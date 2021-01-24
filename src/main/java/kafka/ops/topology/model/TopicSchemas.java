package kafka.ops.topology.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Optional;
import kafka.ops.topology.model.schema.Subject;

@JsonNaming(PropertyNamingStrategy.LowerDotCaseStrategy.class)
public class TopicSchemas {

  private Subject keySubject;
  private Subject valueSubject;

  /**
   * Topic schemas constructor
   *
   * @param keyJsonNode
   * @param keyRecordJsonNode
   * @param keyFormatJsonNode
   * @param keyCompatibilityJsonNode
   * @param valueJsonNode
   * @param valueRecordJsonNode
   * @param valueFormatJsonNode
   * @param valueCompatibilityJsonNode
   */
  public TopicSchemas(
      Optional<JsonNode> keyJsonNode,
      Optional<JsonNode> keyRecordJsonNode,
      Optional<JsonNode> keyFormatJsonNode,
      Optional<JsonNode> keyCompatibilityJsonNode,
      Optional<JsonNode> valueJsonNode,
      Optional<JsonNode> valueRecordJsonNode,
      Optional<JsonNode> valueFormatJsonNode,
      Optional<JsonNode> valueCompatibilityJsonNode) {
    this.keySubject =
        new Subject(
            keyJsonNode,
            keyRecordJsonNode,
            keyFormatJsonNode,
            keyCompatibilityJsonNode,
            Subject.SubjectKind.KEY);
    this.valueSubject =
        new Subject(
            valueJsonNode,
            valueRecordJsonNode,
            valueFormatJsonNode,
            valueCompatibilityJsonNode,
            Subject.SubjectKind.VALUE);
  }

  public TopicSchemas(String keySchemaFile, String valueSchemaFile) {
    this.keySubject = new Subject(keySchemaFile, null, Subject.SubjectKind.KEY);
    this.valueSubject = new Subject(valueSchemaFile, null, Subject.SubjectKind.VALUE);
  }

  public Subject getKeySubject() {
    return keySubject;
  }

  public Subject getValueSubject() {
    return valueSubject;
  }
}
