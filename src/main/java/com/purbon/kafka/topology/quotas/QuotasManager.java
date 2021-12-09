package com.purbon.kafka.topology.quotas;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Quota;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuotasManager {

  private static final Logger LOGGER = LogManager.getLogger(QuotasManager.class);

  private final AdminClient adminClient;
  // private final TopologyBuilderAdminClient adminClient;
  private final Configuration config;

  public QuotasManager(AdminClient adminClient, Configuration config) {
    this.adminClient = adminClient;
    this.config = config;
  }

  public void assignQuotasPrincipal(Collection<Quota> quotas) {
    List<ClientQuotaAlteration> lstQuotasAlteration =
        quotas.stream()
            .map(f -> new QuotasClientBindingsBuilder(f).build())
            .collect(Collectors.toList());

    this.adminClient.alterClientQuotas(lstQuotasAlteration).all();
  }

  public void removeQuotasPrincipal(Collection<User> users) {
    List<ClientQuotaAlteration> lstQuotasRemove =
        users.stream()
            .map(
                f ->
                    new QuotasClientBindingsBuilder(
                            new Quota(
                                f.getPrincipal(),
                                Optional.empty(),
                                Optional.empty(),
                                Optional.empty()))
                        .build())
            .collect(Collectors.toList());
    this.adminClient.alterClientQuotas(lstQuotasRemove);
  }

  private void describeClientQuotas() throws ExecutionException, InterruptedException {
    Map<ClientQuotaEntity, Map<String, Double>> quotasresult =
        this.adminClient.describeClientQuotas(ClientQuotaFilter.all()).entities().get();
  }
}
