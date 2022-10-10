package com.purbon.kafka.topology.clients;

import com.purbon.kafka.topology.model.Artefact;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ArtefactClient {

  String getServer();

  Map<String, Object> add(String content) throws IOException;

  default void addSessionVars(Map<String, String> sessionVars) {
    // empty body
  }

  default Map<String, Object> add(String name, String config) throws IOException {
    throw new IOException("Not implemented");
  }

  default Map<String, Object> update(String name, String config) throws IOException {
    // make update fallback to add, like this KsqlDB and other idempotent APIs will handle
    // updates out of the box.
    return add(name, config);
  }

  default void delete(String label) throws IOException {
    throw new IOException("Not implemented");
  }

  default void delete(String label, String type) throws IOException {
    throw new IOException("Not implemented");
  }

  List<String> list() throws IOException;

  Collection<? extends Artefact> getClusterState() throws IOException;
}
