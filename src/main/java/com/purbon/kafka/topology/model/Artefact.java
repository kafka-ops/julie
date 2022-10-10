package com.purbon.kafka.topology.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Artefact {

  private final String path;

  private final String serverLabel;
  private final String name;
  private final String hash;

  public Artefact(String path, String serverLabel, String name) {
    this(path, serverLabel, name, null);
  }

  public Artefact(String path, String serverLabel, String name, String hash) {
    this.path = path;
    this.serverLabel = serverLabel;
    this.name = name;
    this.hash = hash;
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

  public String getHash() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Artefact)) return false;
    Artefact artefact = (Artefact) o;
    if (serverLabel != null) {
      return getServerLabel().equalsIgnoreCase(artefact.getServerLabel())
          && Objects.equals(name.toLowerCase(), artefact.getName().toLowerCase());
    } else {
      return Objects.equals(name.toLowerCase(), artefact.getName().toLowerCase());
    }
  }

  @Override
  public int hashCode() {
    if (serverLabel != null) {
      return Objects.hash(getServerLabel().toLowerCase(), getName().toLowerCase());
    } else {
      return Objects.hash(getName().toLowerCase());
    }
  }

  @Override
  public String toString() {
    return "Artefact{"
        + "path='"
        + path
        + '\''
        + ", serverLabel='"
        + serverLabel
        + '\''
        + ", name='"
        + name
        + '\''
        + ", hash='"
        + hash
        + '\''
        + '}';
  }
}
