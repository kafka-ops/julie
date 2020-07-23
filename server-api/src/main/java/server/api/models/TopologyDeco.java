package server.api.models;

import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TopologyDeco extends TopologyImpl {

  public Topology asTopology() {
    Topology topology = clone();
    List<Project> projects = getProjects()
        .stream()
        .map(project -> {
          ProjectImpl impl = ((ProjectImpl)project).clone();

          List<Topic> topics = project
              .getTopics()
              .stream()
              .map(topic -> {
                Topic newTopic = ((TopicImpl)topic).clone();
                HashMap<String,String> newConfig = replaceKeys(newTopic.getConfig());
                newTopic.setConfig(newConfig);
                return newTopic;
              })
              .collect(Collectors.toList());
          impl.setTopics(topics);
          return impl;
        }).collect(Collectors.toList());

    topology.setProjects(projects);

    return topology;
  }

  private HashMap<String, String> replaceKeys(HashMap<String, String> config) {
    HashMap<String, String> newConfig = new HashMap<>();
    config.forEach((key, value) -> {
      String newKey = key.replaceAll("_", ".");
      newConfig.put(newKey, value);
    });
    return newConfig;
  }
}
