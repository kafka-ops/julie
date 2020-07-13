package server.api.model.topology;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.TopicManager;
import java.util.HashMap;
import java.util.Map;

public class Topic {

  public static final String DEFAULT_TOPIC_NAME = "default";

  @JsonInclude(Include.NON_EMPTY)
  private String dataType;

  private String name;
  private Map<String, String> config;

  private String project;

  public Topic(String name) {
    this(name, "", new HashMap<>());
  }

  public Topic(String name, String dataType) {
    this(name, dataType, new HashMap<>());
  }

  public Topic(String name, String dataType, HashMap<String, String> config) {
    this.name = name;
    this.dataType = dataType;
    this.config = config;
  }

  public Topic() {
    this(DEFAULT_TOPIC_NAME, "", new HashMap<>());
  }

  public String getName() {
    return name;
  }

  private String toString(String project) {
    StringBuilder sb = new StringBuilder();
    sb.append(project).append(".").append(getName());

    if (!getDataType().isEmpty()) {
      sb.append(".").append(getDataType());
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return toString(project);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  public Map<String, String> rawConfig() {
    getConfig().remove(TopicManager.NUM_PARTITIONS);
    getConfig().remove(TopicManager.REPLICATION_FACTOR);
    return getConfig();
  }

  public String getDataType() {
    return dataType;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getProject() {
    return this.project;
  }
}
