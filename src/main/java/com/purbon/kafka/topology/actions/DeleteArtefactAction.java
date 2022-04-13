package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.TypeArtefact;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    LOGGER.debug(
        String.format(
            "Deleting artefact %s with client %s", artefact.getName(), client.getClass()));

    if (artefact.getClass().isAnnotationPresent(TypeArtefact.class)) {
      TypeArtefact annon = artefact.getClass().getAnnotation(TypeArtefact.class);
      LOGGER.debug("Deleting artefact with type " + annon.name());
      client.delete(artefact.getName(), annon.name());

    } else {
      client.delete(artefact.getName());
    }
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

  @Override
  protected List<Map<String, Object>> detailedProps() {
    Map<String, Object> map = new HashMap<>();
    map.put(
        "resource_name",
        String.format("rn://delete.artefact/%s/%s", getClass().getName(), artefact.getName()));
    map.put("operation", getClass().getName());
    map.put("artefact", artefact.getPath());
    return Collections.singletonList(map);
  }
}
