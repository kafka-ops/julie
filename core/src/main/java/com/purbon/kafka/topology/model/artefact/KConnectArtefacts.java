package com.purbon.kafka.topology.model.artefact;

import com.purbon.kafka.topology.model.Artefacts;
import java.util.ArrayList;
import java.util.List;

public class KConnectArtefacts implements Artefacts {

  private List<KafkaConnectArtefact> connectors;

  public KConnectArtefacts() {
    this(new ArrayList<>());
  }

  public KConnectArtefacts(List<KafkaConnectArtefact> artefacts) {
    this.connectors = artefacts;
  }

  public List<KafkaConnectArtefact> getConnectors() {
    return connectors;
  }

  public void setConnectors(List<KafkaConnectArtefact> connectors) {
    this.connectors = connectors;
  }
}
