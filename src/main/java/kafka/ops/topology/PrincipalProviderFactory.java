package kafka.ops.topology;

import static kafka.ops.topology.TopologyBuilderConfig.CCLOUD_ENV_CONFIG;

import kafka.ops.topology.serviceAccounts.CCloudPrincipalProvider;
import kafka.ops.topology.serviceAccounts.VoidPrincipalProvider;

public class PrincipalProviderFactory {

  private TopologyBuilderConfig config;

  public PrincipalProviderFactory(TopologyBuilderConfig config) {
    this.config = config;
  }

  public PrincipalProvider get() {
    if (config.hasProperty(CCLOUD_ENV_CONFIG)) {
      return new CCloudPrincipalProvider(config);
    } else {
      return new VoidPrincipalProvider();
    }
  }
}
