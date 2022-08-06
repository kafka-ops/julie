package com.purbon.kafka.topology.validation.topology;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.validation.TopologyValidation;

public class CamelCaseNameFormatValidation implements TopologyValidation {

  private String camelCasePattern = "([a-z]+[A-Z]+\\w+)+";

  @Override
  public void valid(Topology topology) throws ValidationException {

    matches(topology.getContext(), "Topology");
    for (Project project : topology.getProjects()) {
      matches(project.getName(), "Project");
      for (Topic topic : project.getTopics()) {
        matches(topic.getName(), "Topic");
      }
    }
  }

  private void matches(String name, String clazz) throws ValidationException {
    if (!name.matches(camelCasePattern)) {
      String msg = String.format("%s name does not follow the camelCase format: %s", clazz, name);
      throw new ValidationException(msg);
    }
  }
}
