package com.purbon.kafka.topology.quotas;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.ExecutionPlan;
import com.purbon.kafka.topology.ExecutionPlanUpdater;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Quota;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuotasManager implements ExecutionPlanUpdater {

  private static final Logger LOGGER = LogManager.getLogger(QuotasManager.class);

  private final TopologyBuilderAdminClient adminClient;
  // private final TopologyBuilderAdminClient adminClient;
  private final Configuration config;

  public QuotasManager(TopologyBuilderAdminClient adminClient, Configuration config) {
    this.adminClient = adminClient;
    this.config = config;
  }

  @Override
  public void updatePlan(ExecutionPlan plan, Map<String, Topology> topologies) throws IOException {
    // TODO: manage deletion of quotas, for now update quotas to a high enough value
    for (Topology topology : topologies.values()) {
      topology.getPlatform().getKafka().getQuotas().ifPresent(adminClient::assignQuotasPrincipal);
    }
  }

  @Override
  public void printCurrentState(PrintStream out) throws IOException {
    try {
      Map<ClientQuotaEntity, Map<String, Double>> clientQuotaEntityMapMap = adminClient.describeClientQuotas();
      clientQuotaEntityMapMap.entrySet().forEach(clientQuotaEntityMapEntry -> {
        out.println(clientQuotaEntityMapEntry.toString());
      });
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
