package com.purbon.kafka.topology.actions.quotas;

import com.purbon.kafka.topology.actions.BaseAction;
import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.model.User;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteQuotasAction extends BaseAction {
  private final TopologyBuilderAdminClient adminClient;
  private final List<User> users;

  public DeleteQuotasAction(TopologyBuilderAdminClient adminClient, List<String> users) {
    this.adminClient = adminClient;
    this.users = users.stream().map(User::new).collect(Collectors.toList());
  }

  @Override
  public void run() throws IOException {
    adminClient.removeQuotasPrincipal(users);
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Users", users);
    map.put("Action", "delete");
    return map;
  }

  @Override
  protected List<Map<String, Object>> detailedProps() {
    return List.of(props());
  }
}
