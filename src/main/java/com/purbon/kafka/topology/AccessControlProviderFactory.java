package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_DEFAULT_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.RBAC_ACCESS_CONTROL_CLASS;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import java.io.IOException;
import java.lang.reflect.Constructor;

public class AccessControlProviderFactory {

  private final TopologyBuilderConfig config;
  private final TopologyBuilderAdminClient builderAdminClient;
  private final MDSApiClientBuilder mdsApiClientBuilder;

  public AccessControlProviderFactory(
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient builderAdminClient,
      MDSApiClientBuilder mdsApiClientBuilder) {
    this.config = config;
    this.builderAdminClient = builderAdminClient;
    this.mdsApiClientBuilder = mdsApiClientBuilder;
  }

  public AccessControlProvider get() throws IOException {

    String accessControlClassName = config.getAccessControlClassName();

    try {
      Class<?> clazz = Class.forName(accessControlClassName);
      switch (accessControlClassName) {
        case ACCESS_CONTROL_DEFAULT_CLASS:
          Constructor<?> aclsProviderConstructor =
              clazz.getConstructor(TopologyBuilderAdminClient.class);
          return (SimpleAclsProvider) aclsProviderConstructor.newInstance(builderAdminClient);
        case RBAC_ACCESS_CONTROL_CLASS:
          Constructor<?> rbacProviderContructor = clazz.getConstructor(MDSApiClient.class);
          MDSApiClient apiClient = mdsApiClientBuilder.build();
          String mdsUser = config.getProperty(MDS_USER_CONFIG);
          String mdsPassword = config.getProperty(MDS_PASSWORD_CONFIG);
          apiClient.login(mdsUser, mdsPassword);
          apiClient.authenticate();
          return (RBACProvider) rbacProviderContructor.newInstance(apiClient);
        default:
          throw new IOException("Unknown access control provided. " + accessControlClassName);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
