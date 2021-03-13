package com.purbon.kafka.topology;

import com.purbon.kafka.topology.api.connect.KConnectApiClient;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.utils.Either;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class KafkaConnectArtefactManager extends ArtefactManager {

  public KafkaConnectArtefactManager(
      ArtefactClient client, Configuration config, String topologyFileOrDir) {
    super(client, config, topologyFileOrDir);
  }

  public KafkaConnectArtefactManager(
      Map<String, KConnectApiClient> clients, Configuration config, String topologyFileOrDir) {
    super(clients, config, topologyFileOrDir);
  }

  @Override
  Collection<? extends Artefact> loadActualClusterStateIfAvailable(ExecutionPlan plan)
      throws IOException {
    return config.fetchStateFromTheCluster() ? getClustersState() : plan.getConnectors();
  }

  private Collection<? extends Artefact> getClustersState() throws IOException {
    List<Either> list =
        clients.values().stream()
            .map(
                client -> {
                  try {
                    return Either.Right(client.getClusterState());
                  } catch (IOException ex) {
                    return Either.Left(ex);
                  }
                })
            .collect(Collectors.toList());

    List<IOException> errors =
        list.stream()
            .filter(Either::isLeft)
            .map(e -> (IOException) e.getLeft().get())
            .collect(Collectors.toList());
    if (errors.size() > 0) {
      throw new IOException(errors.get(0));
    }

    return list.stream()
        .filter(Either::isRight)
        .map(e -> (KafkaConnectArtefact) e.getRight().get())
        .collect(Collectors.toSet());
  }

  @Override
  Set<KafkaConnectArtefact> parseNewArtefacts(Topology topology) {
    return topology.getProjects().stream()
        .flatMap(project -> project.getConnectorArtefacts().stream())
        .collect(Collectors.toSet());
  }

  @Override
  boolean isAllowDelete() {
    return config.allowDelete() || config.isAllowDeleteConnectArtefacts();
  }

  @Override
  String rootPath() {
    return Files.isDirectory(Paths.get(topologyFileOrDir))
        ? topologyFileOrDir
        : new File(topologyFileOrDir).getParent();
  }
}
