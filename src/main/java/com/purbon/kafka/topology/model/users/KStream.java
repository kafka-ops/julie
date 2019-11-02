package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.DynamicUser;
import com.purbon.kafka.topology.model.User;
import java.util.HashMap;
import java.util.List;

public class KStream extends DynamicUser {

  public KStream() {
    super();
  }

  public KStream(String principal, HashMap<String, List<String>> topics) {
    super(principal, topics);
  }

}
