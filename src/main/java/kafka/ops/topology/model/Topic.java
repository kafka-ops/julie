package kafka.ops.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import kafka.ops.topology.TopologyBuilderConfig;
import kafka.ops.topology.model.Impl.TopicImpl;
import kafka.ops.topology.model.users.Consumer;
import kafka.ops.topology.model.users.Producer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonDeserialize(as = TopicImpl.class)
public interface Topic {

  String getName();

  List<TopicSchemas> getSchemas();

  String getPlan();

  void setSchemas(List<TopicSchemas> schemas);

  Map<String, String> getConfig();

  HashMap<String, String> getRawConfig();

  Optional<String> getDataType();

  void addAppConfig(TopologyBuilderConfig appConfig);

  int partitionsCount();

  void setDefaultProjectPrefix(String projectPrefix);

  void setPrefixContext(Map<String, Object> prefixContext);

  short replicationFactor();

  List<Consumer> getConsumers();

  List<Producer> getProducers();

  void setConsumers(List<Consumer> consumers);

  void setProducers(List<Producer> producers);

  Map<String, String> getMetadata();

  void setMetadata(Map<String, String> metadata);

  SubjectNameStrategy getSubjectNameStrategy();
}
