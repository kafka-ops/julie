package com.purbon.kafka.topology;

import com.purbon.kafka.topology.api.ccloud.CCloud;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder;
import com.purbon.kafka.topology.serviceAccounts.CCloudPrincipalProvider;
import com.purbon.kafka.topology.serviceAccounts.VoidPrincipalProvider;

import java.io.IOException;

import static com.purbon.kafka.topology.TopologyBuilderConfig.*;
import static com.purbon.kafka.topology.TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS;

public class PrincipalProviderFactory {

  private TopologyBuilderConfig config;
  private CCloud cCloud;

  public PrincipalProviderFactory(TopologyBuilderConfig config, CCloud cCloud) {
    this.config = config;
    this.cCloud = cCloud;
  }

  public PrincipalProvider get() throws IOException {

    String principleProviderClass = config.getPrincipleProviderImplementationClass();

    try {
      if (principleProviderClass.equalsIgnoreCase(PRINCIPLE_PROVIDER_DEFAULT_CLASS)) {
        return new VoidPrincipalProvider();
      } else if (principleProviderClass.equalsIgnoreCase(CONFLUENT_CLOUD_PRINCIPLE_PROVIDER_CLASS)) {
        return new CCloudPrincipalProvider(cCloud);
      } else {
        throw new IOException(principleProviderClass + " Unknown principle provider provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
