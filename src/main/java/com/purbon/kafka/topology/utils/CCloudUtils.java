package com.purbon.kafka.topology.utils;

import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.ccloud.CCloudCLI;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CCloudUtils {

  private final CCloudCLI cli;
  private String env;
  private Map<String, ServiceAccount> serviceAccounts;
  private boolean warmed;

  public CCloudUtils(CCloudCLI cli, TopologyBuilderConfig config) {
    this.cli = cli;
    this.env = config.useConfuentCloud() ? config.getConfluentCloudEnv() : "";
    this.serviceAccounts = new HashMap<>();
    this.warmed = false;
  }

  public void warmup() throws IOException {
    if (warmed) {
      return;
    }

    if (serviceAccounts.isEmpty()) {
      if (env.isEmpty()) {
        throw new IOException("Environment can't be empty");
      }
      cli.setEnvironment(env);
      this.serviceAccounts = cli.serviceAccounts();
      this.warmed = true;
    }
  }

  public int translate(String name) {
    return serviceAccounts.containsKey(name) ? serviceAccounts.get(name).getId() : -1;
  }
}
