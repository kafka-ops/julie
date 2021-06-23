package com.purbon.kafka.topology.actions.topics;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.schema.Subject;
import com.purbon.kafka.topology.model.schema.TopicSchemas;
import com.purbon.kafka.topology.schemas.SchemaRegistryManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegisterSchemaAction extends BaseAction {

  private static final Logger LOGGER = LogManager.getLogger(RegisterSchemaAction.class);

  private final Topic topic;
  private final String fullTopicName;
  private final SchemaRegistryManager schemaRegistryManager;

  public RegisterSchemaAction(
      SchemaRegistryManager schemaRegistryManager, Topic topic, String fullTopicName) {
    this.topic = topic;
    this.fullTopicName = fullTopicName;
    this.schemaRegistryManager = schemaRegistryManager;
  }

  public String getTopic() {
    return fullTopicName;
  }

  @Override
  public void run() throws IOException {
    registerSchemas(topic, fullTopicName);
  }

  private void registerSchemas(Topic topic, String fullTopicName) throws IOException {
    LOGGER.debug(String.format("Register schemas for topic %s", fullTopicName));

    for (TopicSchemas schema : topic.getSchemas()) {
      registerSchemaIfExists(schema.getKeySubject(), topic);
      registerSchemaIfExists(schema.getValueSubject(), topic);
    }
  }

  private void registerSchemaIfExists(Subject subject, Topic topic) throws IOException {
    if (subject.hasSchemaFile()) {
      String keySchemaFile = subject.getSchemaFile();
      String subjectName = subject.buildSubjectName(topic);
      schemaRegistryManager.register(subjectName, keySchemaFile, subject.getFormat());
      setCompatibility(subjectName, subject.getOptionalCompatibility());
    }
  }

  private void setCompatibility(String subjectName, Optional<String> compatibilityOptional) {
    compatibilityOptional.ifPresent(
        compatibility -> schemaRegistryManager.setCompatibility(subjectName, compatibility));
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Topic", fullTopicName);
    map.put("Schemas", "TODO: Schemas registered...");
    return map;
  }
}
