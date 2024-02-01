package com.purbon.kafka.topology.quotas;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.ExecutionPlanUpdater;
import com.purbon.kafka.topology.actions.quotas.CreateQuotasAction;
import com.purbon.kafka.topology.actions.quotas.DeleteQuotasAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Quota;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuotasManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(QuotasManager.class);

  private final TopologyBuilderAdminClient adminClient;
  private final Configuration config;

  public QuotasManager(TopologyBuilderAdminClient adminClient, Configuration config) {
    this.adminClient = adminClient;
    this.config = config;
  }

  @Override
  public void updatePlan(ExecutionPlan plan, Map<String, Topology> topologies) throws IOException {
    // Get current quotas
    try {
      Map<ClientQuotaEntity, Map<String, Double>> currentQuotas =
          adminClient.describeClientQuotas();
      Map<String, Map<String, Double>> currentUsersWithQuotas =
          currentQuotas.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      entry -> entry.getKey().entries().get(ClientQuotaEntity.USER),
                      Map.Entry::getValue));

      Set<String> usersWithQuotasInTopology = new HashSet<>();
      for (Topology topology : topologies.values()) {
        topology
            .getPlatform()
            .getKafka()
            .getQuotas()
            .ifPresent(
                quotas -> {
                  quotas.forEach(
                      quota -> {
                        String principal = quota.getPrincipal();
                        Map<String, Double> currentQuotasForPrincipal =
                            currentUsersWithQuotas.get(principal);
                        // Check quotas with already existing one
                        if (currentQuotasForPrincipal == null
                            || isQuotaUpdated(currentQuotasForPrincipal, quota)) {
                          plan.add(new CreateQuotasAction(adminClient, quotas));
                        }
                        usersWithQuotasInTopology.add(principal);
                      });
                });
      }
      // Check for deletion, no prefixes here, all quotas should be put in one unique file
      if (config.isAllowDeleteQuotas()) {
        usersWithQuotasInTopology.forEach(currentUsersWithQuotas::remove);
        if (!currentUsersWithQuotas.isEmpty()) {
          plan.add(
              new DeleteQuotasAction(
                  adminClient, new ArrayList<>(currentUsersWithQuotas.keySet())));
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isQuotaUpdated(Map<String, Double> currentQuotasForPrincipal, Quota quota) {
    Double consumerByteRate = currentQuotasForPrincipal.get("consumer_byte_rate");
    if (quota.getConsumer_byte_rate().isEmpty() && consumerByteRate != null
        || consumerByteRate == null && quota.getConsumer_byte_rate().isPresent()) {
      return true;
    } else if (consumerByteRate != null && quota.getConsumer_byte_rate().isPresent()) {
      if (!consumerByteRate.equals(quota.getConsumer_byte_rate().get())) {
        return true;
      }
    }

    Double producerByteRate = currentQuotasForPrincipal.get("producer_byte_rate");
    if (quota.getProducer_byte_rate().isEmpty() && producerByteRate != null
        || producerByteRate == null && quota.getProducer_byte_rate().isPresent()) {
      return true;
    } else if (producerByteRate != null && quota.getProducer_byte_rate().isPresent()) {
      if (!producerByteRate.equals(quota.getProducer_byte_rate().get())) {
        return true;
      }
    }

    Double requestPercentage = currentQuotasForPrincipal.get("request_percentage");
    if (quota.getRequest_percentage().isEmpty() && requestPercentage != null
        || requestPercentage == null && quota.getRequest_percentage().isPresent()) {
      return true;
    } else if (requestPercentage != null && quota.getRequest_percentage().isPresent()) {
      if (!requestPercentage.equals(quota.getRequest_percentage().get())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void printCurrentState(PrintStream out) throws IOException {
    out.println("List of Quotas:");
    try {
      Map<ClientQuotaEntity, Map<String, Double>> clientQuotaEntityMapMap =
          adminClient.describeClientQuotas();
      clientQuotaEntityMapMap
          .entrySet()
          .forEach(
              clientQuotaEntityMapEntry -> {
                out.println(clientQuotaEntityMapEntry.toString());
              });
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
