package kafka.ops.topology.model.users;

import kafka.ops.topology.model.User;
import java.util.List;

public class Schemas extends User {

  private List<String> subjects;

  public Schemas() {
    super("");
  }

  public List<String> getSubjects() {
    return subjects;
  }

  public void setSubjects(List<String> subjects) {
    this.subjects = subjects;
  }
}
