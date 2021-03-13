package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteArtefactAction extends BaseAction {

  private static final Logger LOGGER = LogManager.getLogger(DeleteArtefactAction.class);

  private ArtefactClient client;
  private Artefact artefact;

  public DeleteArtefactAction(ArtefactClient client, Artefact artefact) {
    this.client = client;
    this.artefact = artefact;
  }

  @Override
  public void run() throws IOException {
    LOGGER.debug(String.format("Deleting artefact %s with client %s", artefact.getName(), client.getClass()));
    client.delete(artefact.getName());
  }

  public Artefact getArtefact() {
    return artefact;
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Artefact", artefact.getPath());
    return map;
  }
}
