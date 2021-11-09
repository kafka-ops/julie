package com.purbon.kafka.topology.api.connect;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.utils.BasicAuth;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KConnectApiClient extends JulieHttpClient implements ArtefactClient {

  public KConnectApiClient(String server, Configuration config) {
    this(server, "", config);
  }

  public KConnectApiClient(String server, String label, Configuration config) {
    super(server, Optional.of(config));
    // configure basic authentication if available
    var basicAuths = config.getServersBasicAuthMap();
    if (basicAuths.containsKey(label)) {
      String[] values = basicAuths.get(label).split(":");
      setBasicAuth(new BasicAuth(values[0], values[1]));
    }
  }

  @Override
  public String getServer() {
    return server;
  }

  @Override
  public Map<String, Object> add(String content) throws IOException {
    throw new IOException("Not implemented in this context");
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

  @Override
  public Map<String, Object> add(String name, String config) throws IOException {
    String url = String.format("/connectors/%s/config", name);

    var map = JSON.toMap(config);
    if (mayBeAConfigRecord(map)) {
      var content = map.get("config");
      if (!name.equalsIgnoreCase(map.get("name").toString())) {
        throw new IOException("Trying to add a connector with a different name as in the topology");
      }
      config = JSON.asString(content);
    }

    String response = doPut(url, config);
    return JSON.toMap(response);
  }

  private boolean mayBeAConfigRecord(Map<String, Object> map) {
    var keySet = map.keySet();
    return keySet.contains("config") && keySet.contains("name") && keySet.size() == 2;
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

  @Override
  public String toString() {
    return "KConnectApiClient{" + server + "}";
  }
}
