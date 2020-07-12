package server.api.model.topology;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;

@BsonDiscriminator
public class User {

  @BsonProperty(value = "principal")
  private String principal;

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
}
