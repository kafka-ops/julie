package com.purbon.kafka.topology.model;

import java.util.Collections;
import java.util.List;

public class PlatformSystem<T extends User> {

  private List<T> accessControlLists;
  private List<? extends Artefact> artefacts;

  public PlatformSystem() {
    this(Collections.emptyList(), Collections.emptyList());
  }

  public PlatformSystem(List<T> accessControlLists) {
    this(accessControlLists, Collections.emptyList());
  }

  public PlatformSystem(List<T> accessControlLists, List<? extends Artefact> artefacts) {
    this.accessControlLists = accessControlLists;
    this.artefacts = artefacts;
  }

  public List<T> getAccessControlLists() {
    return accessControlLists;
  }

  public List<? extends Artefact> getArtefacts() {
    return artefacts;
  }
}
