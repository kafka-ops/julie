package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.List;

public class KStream extends User {

  private HashMap<String, List<String>> topics;

  public static final String READ_TOPICS = "read";
  public static final String WRITE_TOPICS = "write";

  public KStream() {
    super();
  }

  public KStream(String principal, HashMap<String, List<String>> topics) {
    super(principal);
    this.topics = topics;
  }

  public HashMap<String, List<String>> getTopics() {
    return topics;
  }

  public void setTopics(HashMap<String, List<String>> topics) {
    this.topics = topics;
  }
}
