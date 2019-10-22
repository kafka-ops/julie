package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.List;

public class KStream extends User {

  private HashMap<String, List<String>> topics;

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
