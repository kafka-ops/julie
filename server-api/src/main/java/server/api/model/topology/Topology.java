package server.api.model.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topology {

  private String team;

  private Map<String, String> others;

  private List<Project> projects;
  private List<String> order;
  private Platform platform;

  public Topology() {
    this.team = "default";
    this.others = new HashMap<>();
    this.order = new ArrayList<>();
    this.projects = new ArrayList<>();
    this.platform = new Platform();
  }

  public String getTeam() {
    return team;
  }

  public void setTeam(String team) {
    this.team = team;
  }

  public List<Project> getProjects() {
    return projects;
  }

  public void addProject(Project project) {
    project.setTopology(this);
    this.projects.add(project);
  }

  public void setProjects(List<Project> projects) {
    this.projects = projects;
  }

  public String buildNamePrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append(getTeam());
    for (String key : order) {
      String value = others.get(key);
      sb.append(".");
      sb.append(value);
    }
    return sb.toString();
  }

  public void addOther(String fieldName, String value) {
    order.add(fieldName);
    others.put(fieldName, value);
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public Platform getPlatform() {
    return this.platform;
  }

}
