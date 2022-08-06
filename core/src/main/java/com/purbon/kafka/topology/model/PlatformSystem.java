package com.purbon.kafka.topology.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlatformSystem<T extends User> {

  private List<T> accessControlLists;
  private Optional<Artefacts> artefacts;

  public PlatformSystem() {
    this(Collections.emptyList(), null);
  }

  public PlatformSystem(List<T> accessControlLists) {
    this(accessControlLists, null);
  }

  public PlatformSystem(List<T> accessControlLists, Artefacts artefacts) {
    this.accessControlLists = accessControlLists;
    this.artefacts = Optional.ofNullable(artefacts);
  }

  public List<T> getAccessControlLists() {
    return accessControlLists;
  }

  public Optional<Artefacts> getArtefacts() {
    return artefacts;
  }
}
