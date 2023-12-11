package com.purbon.kafka.topology.actions.quotas;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.users.Quota;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateQuotasAction extends BaseAction {
  private final TopologyBuilderAdminClient adminClient;
  private final List<Quota> quotas;

  public CreateQuotasAction(TopologyBuilderAdminClient adminClient, List<Quota> quotas) {
    this.adminClient = adminClient;
    this.quotas = quotas;
  }

  @Override
  public void run() throws IOException {
    adminClient.assignQuotasPrincipal(quotas);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Quotas", quotas);
    map.put("Action", "create");
    return map;
  }

  @Override
  protected List<Map<String, Object>> detailedProps() {
    return List.of(props());
  }
}
