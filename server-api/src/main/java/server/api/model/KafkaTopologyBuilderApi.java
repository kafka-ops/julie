package server.api.model;

import io.micronaut.core.annotation.Introspected;
import javax.validation.constraints.NotBlank;

@Introspected
public class KafkaTopologyBuilderApi {

  private String name;
  private String version;

  public KafkaTopologyBuilderApi() {
    name = "Kafka Topology Builder";
    version = "1.0.0";
  }
  @NotBlank
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @NotBlank
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
