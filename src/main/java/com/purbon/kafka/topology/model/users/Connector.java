package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.List;

public class Connector extends User {

  private HashMap<String, List<String>> topics;

  public static final String READ_TOPICS = "read";
  public static final String WRITE_TOPICS = "write";

  public Connector() {
    super();
    this.topics = new HashMap<>();
  }

  public Connector(String principal) {
    super(principal);
    this.topics = new HashMap<>();
  }

  public HashMap<String, List<String>> getTopics() {
    return topics;
  }

  public void setTopics(HashMap<String, List<String>> topics) {
    this.topics = topics;
  }
}
