package com.purbon.kafka.topology.model;

import java.util.Objects;

public abstract class Artefact {

  private String path;
  private String serverLabel;
  private String name;

  public Artefact(String path, String serverLabel, String name) {
    this.path = path;
    this.serverLabel = serverLabel;
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public String getServerLabel() {
    return serverLabel;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Artefact)) return false;
    Artefact artefact = (Artefact) o;
    return getServerLabel().equals(artefact.getServerLabel())
        && Objects.equals(name, artefact.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getServerLabel(), getName());
  }
}
