package server.api.model.topology.users;

import server.api.model.topology.DynamicUser;
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
