package com.purbon.kafka.topology;

import com.purbon.kafka.topology.actions.CreateArtefactAction;
import com.purbon.kafka.topology.actions.DeleteArtefactAction;
import com.purbon.kafka.topology.actions.SyncArtefactAction;
import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.exceptions.RemoteValidationException;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.artefact.KsqlVarsArtefact;
import com.purbon.kafka.topology.model.artefact.TypeArtefact;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages Artefacts as defined within the context of the filter class */
public abstract class ArtefactManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(ArtefactManager.class);

  protected Map<String, ArtefactClient> clients;
  protected Configuration config;
  protected String topologyFileOrDir;

  public ArtefactManager(ArtefactClient client, Configuration config, String topologyFileOrDir) {
    this(Collections.singletonMap("default", client), config, topologyFileOrDir);
  }

  public ArtefactManager(
      Map<String, ? extends ArtefactClient> clients,
      Configuration config,
      String topologyFileOrDir) {
    this.clients = Collections.unmodifiableMap(clients);
    this.config = config;
    this.topologyFileOrDir = topologyFileOrDir;
  }

  private boolean findKsqlVarsArtefact(Artefact artefact) {
    return Optional.ofNullable(artefact.getClass().getAnnotation(TypeArtefact.class))
        .map(x -> x.name().equals("VARS"))
        .orElse(false);
  }

  @Override
  public void updatePlan(ExecutionPlan plan, Map<String, Topology> topologies) throws IOException {
    Collection<? extends Artefact> currentArtefacts = loadActualClusterStateIfAvailable(plan);

    Set<Artefact> artefacts = new HashSet<>();

    for (Topology topology : topologies.values()) {
      Set<? extends Artefact> entryArtefacts = parseNewArtefacts(topology);

      final var kSqlVarsArtefact =
          ((Optional<KsqlVarsArtefact>)
                  entryArtefacts.stream().filter(this::findKsqlVarsArtefact).findFirst())
              .orElseGet(() -> new KsqlVarsArtefact(Collections.emptyMap()));
      entryArtefacts.removeIf(this::findKsqlVarsArtefact);

      for (Artefact artefact : entryArtefacts) {
        Optional<? extends Artefact> existingArtefactOpt =
            currentArtefacts.stream().filter(ea -> ea.equals(artefact)).findAny();
        if (existingArtefactOpt.isEmpty()) {
          ArtefactClient client = selectClient(artefact);

          if (client == null) {
            throw new IOException(
                "The Artefact "
                    + artefact.getName()
                    + " require a non configured client, please check our configuration");
          }
          client.addSessionVars(kSqlVarsArtefact.getSessionVars());
          plan.add(new CreateArtefactAction(client, rootPath(), currentArtefacts, artefact));
        } else {
          Artefact existingArtefact = existingArtefactOpt.get();
          if (!Objects.equals(existingArtefact.getHash(), artefact.getHash())) {
            ArtefactClient client = selectClient(artefact);
            if (client == null) {
              throw new IOException(
                  "The Artefact "
                      + artefact.getName()
                      + " require a non configured client, please check our configuration");
            }
            plan.add(new SyncArtefactAction(client, rootPath(), artefact));
          }
        }
        artefacts.add(artefact);
      }
    }

    if (isAllowDelete()) {
      List<? extends Artefact> toBeDeleted = findArtefactsToBeDeleted(currentArtefacts, artefacts);

      if (toBeDeleted.size() > 0) {
        LOGGER.debug("Artefacts to be deleted: " + StringUtils.join(toBeDeleted, ","));
        for (Artefact artefact : toBeDeleted) {
          ArtefactClient client = selectClient(artefact);
          if (client == null) {
            throw new IOException(
                "The Artefact "
                    + artefact.getName()
                    + " require a non configured client, please check our configuration");
          }
          plan.add(new DeleteArtefactAction(client, artefact));
        }
      }
    }
  }

  protected List<? extends Artefact> findArtefactsToBeDeleted(
      Collection<? extends Artefact> currentArtefacts, Set<Artefact> artefacts) {
    return currentArtefacts.stream()
        .filter(a -> !artefacts.contains(a))
        .collect(Collectors.toList());
  }

  protected ArtefactClient selectClient(Artefact artefact) {
    ArtefactClient defaultClient = clients.containsKey("default") ? clients.get("default") : null;
    return clients.getOrDefault(artefact.getServerLabel(), defaultClient);
  }

  protected Collection<? extends Artefact> loadActualClusterStateIfAvailable(ExecutionPlan plan)
      throws IOException {
    var currentState = config.fetchStateFromTheCluster() ? getClustersState() : getLocalState(plan);

    if (!config.shouldVerifyRemoteState()) {
      LOGGER.warn(
          "Remote state verification disabled, this is not a good practice, be aware"
              + "in future versions, this check is going to become mandatory.");
    }

    if (config.shouldVerifyRemoteState() && !config.fetchStateFromTheCluster()) {
      // should detect if there are divergences between the local cluster state and the current
      // status in the cluster
      detectDivergencesInTheRemoteCluster(plan);
    }

    return currentState;
  }

  private void detectDivergencesInTheRemoteCluster(ExecutionPlan plan) throws IOException {
    var remoteArtefacts = getClustersState();

    var delta =
        getLocalState(plan).stream()
            .filter(localArtifact -> !remoteArtefacts.contains(localArtifact))
            .collect(Collectors.toList());

    if (delta.size() > 0) {
      String errorMessage =
          "Your remote state has changed since the last execution, these Artefact(s): "
              + StringUtils.join(delta, ",")
              + " are in your local state, but not in the cluster, please investigate!";
      LOGGER.error(errorMessage);
      throw new RemoteValidationException(errorMessage);
    }
  }

  protected abstract Collection<? extends Artefact> getLocalState(ExecutionPlan plan);

  protected abstract Collection<? extends Artefact> getClustersState() throws IOException;

  abstract Set<? extends Artefact> parseNewArtefacts(Topology topology);

  abstract boolean isAllowDelete();

  abstract String rootPath();
}
