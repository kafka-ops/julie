package kafka.ops.topology.model;

import java.util.Map;

public class User {

  private String principal;
  private Map<String, String> metadata;

  public User() {
    this("");
  }

  public User(String principal) {
    this.principal = principal;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
