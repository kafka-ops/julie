package com.purbon.kafka.topology.model;

import java.util.List;

public interface Topology {

  String getContext();

  void setContext(String context);

  List<Project> getProjects();

  void addProject(Project project);

  void setProjects(List<Project> projects);

  String buildNamePrefix();

  void addOther(String fieldName, String value);

  void setPlatform(Platform platform);

  Platform getPlatform();

  Boolean isEmpty();
}
