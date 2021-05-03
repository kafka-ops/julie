package com.purbon.kafka.topology.api.ksql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KsqlArtefact;
import com.purbon.kafka.topology.utils.JSON;
import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import io.confluent.ksql.api.client.QueryInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KsqlApiClient implements ArtefactClient {

  private String server;
  private Client client;

  public KsqlApiClient(String server) {
    this.server = server;
    String port = server.split(":")[1].strip();
    ClientOptions options =
        ClientOptions.create()
            .setHost(server.split(":")[0].strip())
            .setPort(Integer.parseInt(port));
    client = Client.create(options);
  }

  @Override
  public String getServer() {
    return server;
  }

  @Override
  public Map<String, Object> add(String sql) throws IOException {
    client.executeStatement(sql);
    return Collections.emptyMap();
  }

  @Override
  public void delete(String id) throws IOException {
    client.terminatePushQuery(id);
  }

  @Override
  public List<String> list() throws IOException {
    List<QueryInfo> queryInfos;
    try {
      queryInfos = client.listQueries().get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    return queryInfos.stream()
        .map(
            new Function<QueryInfo, KsqlArtefact>() {
              @Override
              public KsqlArtefact apply(QueryInfo queryInfo) {
                return new KsqlArtefact("", queryInfo.getId(), server);
              }
            })
        .map(
            new Function<KsqlArtefact, String>() {
              @Override
              public String apply(KsqlArtefact ksqlArtefact) {
                try {
                  return JSON.asString(ksqlArtefact);
                } catch (JsonProcessingException e) {
                  e.printStackTrace();
                  return "";
                }
              }
            })
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  @Override
  public Collection<? extends Artefact> getClusterState() throws IOException {
    return list().stream()
        .map(
            e -> {
              try {
                return (KsqlArtefact) JSON.toObject(e, KsqlArtefact.class);
              } catch (JsonProcessingException ex) {
                ex.printStackTrace();
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
