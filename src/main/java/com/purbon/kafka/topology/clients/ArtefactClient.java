package com.purbon.kafka.topology.clients;

import com.purbon.kafka.topology.model.Artefact;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ArtefactClient {

  String getServer();

  Map<String, Object> add(String content) throws IOException;

  void delete(String label) throws IOException;

  List<String> list() throws IOException;

  Collection<? extends Artefact> getClusterState() throws IOException;
}
