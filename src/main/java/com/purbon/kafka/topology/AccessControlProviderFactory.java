package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;

import java.io.IOException;
import java.lang.reflect.Constructor;

import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;

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
    try {
      Class<? extends AccessControlProvider> cls = this.getAccessControlClass();
      if (SimpleAclsProvider.class.isAssignableFrom(cls)) {
        Constructor<?> aclsProviderConstructor =
            cls.getConstructor(TopologyBuilderAdminClient.class);
        return (SimpleAclsProvider) aclsProviderConstructor.newInstance(builderAdminClient);
      } else if (RBACProvider.class.isAssignableFrom(cls)) {
        Constructor<?> rbacProviderContructor = cls.getConstructor(MDSApiClient.class);
        MDSApiClient apiClient = mdsApiClientBuilder.build();
        String mdsUser = config.getString(MDS_USER_CONFIG);
        String mdsPassword = config.getString(MDS_PASSWORD_CONFIG);
        apiClient.login(mdsUser, mdsPassword);
        apiClient.authenticate();
        return (RBACProvider) rbacProviderContructor.newInstance(apiClient);
      } else {
        throw new IOException(cls.getName() + " Unknown access control provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private Class<? extends AccessControlProvider> getAccessControlClass()throws ClassNotFoundException {
    return config.getCls(AccessControlProvider.class, ACCESS_CONTROL_IMPLEMENTATION_CLASS, SimpleAclsProvider.class);
  }
}
