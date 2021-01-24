package kafka.ops.topology.model.users.platform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import kafka.ops.topology.model.User;

public class Kafka {

  private Optional<List<User>> instances;
  private Optional<Map<String, List<User>>> rbac;

  public Kafka() {
    instances = Optional.empty();
    rbac = Optional.empty();
  }

  public Optional<List<User>> getInstances() {
    return instances;
  }

  public void setInstances(Optional<List<User>> instances) {
    this.instances = instances;
  }

  public Optional<Map<String, List<User>>> getRbac() {
    return rbac;
  }

  public void setRbac(Optional<Map<String, List<User>>> rbac) {
    this.rbac = rbac;
  }
}
