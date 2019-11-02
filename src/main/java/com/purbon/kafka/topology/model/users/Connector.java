package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.DynamicUser;
import java.util.HashMap;

public class Connector extends DynamicUser {

  public Connector() {
    super();
  }

  public Connector(String principal) {
    super(principal, new HashMap<>());
  }

}
