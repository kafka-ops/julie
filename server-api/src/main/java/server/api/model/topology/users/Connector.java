package server.api.model.topology.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import server.api.model.topology.DynamicUser;
import java.util.HashMap;
import java.util.List;

public class Connector extends DynamicUser {

  public static final String DEFAULT_CONNECT_STATUS_TOPIC = "connect-status";
  public static final String DEFAULT_CONNECT_OFFSET_TOPIC = "connect-offsets";
  public static final String DEFAULT_CONNECT_CONFIGS_TOPIC = "connect-configs";
  public static final String DEFAULT_CONNECT_GROUP = "connect-cluster";

  private static final String DEFAULT_CONNECT_CLUSTER_ID = "connect-cluster";

  @JsonInclude(Include.NON_EMPTY)
  private String status_topic;

  @JsonInclude(Include.NON_EMPTY)
  private String offset_topic;

  @JsonInclude(Include.NON_EMPTY)
  private String configs_topic;

  @JsonInclude(Include.NON_EMPTY)
  private String group;

  @JsonInclude(Include.NON_EMPTY)
  private String cluster_id;

  public Connector() {
    this(DEFAULT_CONNECT_STATUS_TOPIC,DEFAULT_CONNECT_OFFSET_TOPIC,DEFAULT_CONNECT_CONFIGS_TOPIC, DEFAULT_CONNECT_GROUP, DEFAULT_CONNECT_CLUSTER_ID);
  }

  public Connector(String status_topic, String offset_topic, String configs_topic,
      String group, String cluster_id) {
    this.status_topic = status_topic;
    this.offset_topic = offset_topic;
    this.configs_topic = configs_topic;
    this.group = group;
    this.cluster_id = cluster_id;
  }

  public Connector(String principal,
      HashMap<String, List<String>> topics, String status_topic, String offset_topic,
      String configs_topic, String group, String cluster_id) {
    super(principal, topics);
    this.status_topic = status_topic;
    this.offset_topic = offset_topic;
    this.configs_topic = configs_topic;
    this.group = group;
    this.cluster_id = cluster_id;
  }

  public String getStatus_topic() {
    return status_topic;
  }

  public void setStatus_topic(String status_topic) {
    this.status_topic = status_topic;
  }

  public String getOffset_topic() {
    return offset_topic;
  }

  public void setOffset_topic(String offset_topic) {
    this.offset_topic = offset_topic;
  }

  public String getConfigs_topic() {
    return configs_topic;
  }

  public void setConfigs_topic(String configs_topic) {
    this.configs_topic = configs_topic;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getCluster_id() {
    return cluster_id;
  }

  public void setCluster_id(String cluster_id) {
    this.cluster_id = cluster_id;
  }
}
