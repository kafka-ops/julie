package server.api.model.topology.users;

import java.util.Optional;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import server.api.model.topology.User;

@BsonDiscriminator
public class SchemaRegistry extends User {

  @BsonProperty(value = "topic")
  private Optional<String> topic;
  @BsonProperty(value = "group")
  private Optional<String> group;

  public SchemaRegistry() {
    this("");
  }

  public SchemaRegistry(String principal) {
    this(principal, Optional.empty(), Optional.empty());
  }

  public SchemaRegistry(String principal, Optional<String> topic, Optional<String> group) {
    super(principal);
    this.topic = topic;
    this.group = group;
  }


  public void setTopic(Optional<String> topic) {
    this.topic = topic;
  }

  public void setGroup(Optional<String> group) {
    this.group = group;
  }

  public Optional<String> getTopic() {
    return topic;
  }

  public Optional<String> getGroup() {
    return group;
  }
}
