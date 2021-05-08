package com.purbon.kafka.topology.api.ksql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KsqlArtefact;
import com.purbon.kafka.topology.utils.JSON;
import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import io.confluent.ksql.api.client.QueryInfo;
import io.confluent.ksql.api.client.StreamInfo;
import io.confluent.ksql.api.client.TableInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KsqlApiClient implements ArtefactClient {

  private String server;
  private Integer port;
  private Client client;

  public static String QUERY_TYPE = "query";
  public static String STREAM_TYPE = "stream";
  public static String TABLE_TYPE = "table";

  public KsqlApiClient(String server, Integer port) {
    this.server = server;
    this.port = port;
    ClientOptions options =
        ClientOptions.create().setHost(server.split(":")[0].strip()).setPort(port);
    client = Client.create(options);
  }

  @Override
  public String getServer() {
    return server + ":" + port;
  }

  @Override
  public Map<String, Object> add(String sql) throws IOException {
    try {
      var result = client.executeStatement(sql).get();
      return new QueryResponse(result).asMap();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String id) throws IOException {
    delete(id, "STREAM");
    delete(id, "TABLE");
  }

  public void delete(String id, String type) throws IOException {
    try {
      if (STREAM_TYPE.equalsIgnoreCase(type) || TABLE_TYPE.equalsIgnoreCase(type)) {
        String sql = String.format("DROP %s IF EXISTS %s;", type.toUpperCase(), id);
        client.executeStatement(sql).get();
      } else {
        client.terminatePushQuery(id).get();
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new IOException(e);
    }
  }

  @Override
  public List<String> list() throws IOException {
    return Stream.of(listStreams(), listTables())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public List<String> listQuery() throws IOException {
    List<QueryInfo> infos;
    try {
      infos = client.listQueries().get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    return infos.stream()
        .map(info -> new KsqlArtefact("", server, info.getId()))
        .map(artefactToString())
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  public List<String> listTables() throws IOException {
    List<TableInfo> infos;
    try {
      infos = client.listTables().get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    return infos.stream()
        .map(tableInfo -> new KsqlArtefact("", server, tableInfo.getName()))
        .map(artefactToString())
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  private Function<KsqlArtefact, String> artefactToString() {
    return ksqlArtefact -> {
      try {
        return JSON.asString(ksqlArtefact);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        return "";
      }
    };
  }

  public List<String> listStreams() throws IOException {
    List<StreamInfo> infos;
    try {
      infos = client.listStreams().get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    return infos.stream()
        .map(queryInfo -> new KsqlArtefact("", server, queryInfo.getName()))
        .map(artefactToString())
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
