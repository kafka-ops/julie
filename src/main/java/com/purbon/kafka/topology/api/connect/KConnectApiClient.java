package com.purbon.kafka.topology.api.connect;

import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KConnectApiClient extends JulieHttpClient implements ArtefactClient {

  public KConnectApiClient(String server) {
    super(server);
  }

  @Override
  public String getServer() {
    return server;
  }

  public List<String> list() throws IOException {
    Response response = doGet("/connectors");
    return JSON.toArray(response.getResponseAsString());
  }

  @Override
  public Collection<? extends Artefact> getClusterState() throws IOException {
    return list().stream()
        .map(connector -> new KafkaConnectArtefact("", server, connector))
        .collect(Collectors.toList());
  }

  public Map<String, Object> add(String config) throws IOException {
    String response = doPost("/connectors", config);
    return JSON.toMap(response);
  }

  public void delete(String connector) throws IOException {
    doDelete("/connectors/" + connector + "/", "");
  }

  public String status(String connectorName) throws IOException {
    Response response = doGet("/connectors/" + connectorName + "/status");
    Map<String, Object> map = JSON.toMap(response.getResponseAsString());

    if (map.containsKey("error_code")) {
      throw new IOException(map.get("message").toString());
    }

    return ((Map<String, String>) map.get("connector")).get("state");
  }

  public void pause(String connectorName) throws IOException {
    doPut("/connectors/" + connectorName + "/pause");
  }
}
