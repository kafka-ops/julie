package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.TopologyBuilderConfig.*;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.ccloud.CCloud;
import com.purbon.kafka.topology.api.mds.MDSApiClient;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;
import com.purbon.kafka.topology.roles.CCloudAclsProvider;
import com.purbon.kafka.topology.roles.RBACProvider;
import com.purbon.kafka.topology.roles.SimpleAclsProvider;
import com.purbon.kafka.topology.roles.acls.AclsBindingsBuilder;
import com.purbon.kafka.topology.roles.rbac.RBACBindingsBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class AccessControlProviderFactory {

  private final TopologyBuilderConfig config;
  private final TopologyBuilderAdminClient builderAdminClient;
  private final CCloud cCloud;
  private final MDSApiClientBuilder mdsApiClientBuilder;

  public AccessControlProviderFactory(
      TopologyBuilderConfig config,
      TopologyBuilderAdminClient builderAdminClient,
      CCloud cCloud,
      MDSApiClientBuilder mdsApiClientBuilder) {
    this.config = config;
    this.builderAdminClient = builderAdminClient;
    this.cCloud = cCloud;
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
        case CONFLUENT_CLOUD_CONTROL_CLASS:
          Constructor<?> ccloudProviderConstructor =
              clazz.getConstructor(TopologyBuilderAdminClient.class, CCloud.class);
          return (CCloudAclsProvider)
              ccloudProviderConstructor.newInstance(builderAdminClient, cCloud);
        case RBAC_ACCESS_CONTROL_CLASS:
          Constructor<?> rbacProviderContructor = clazz.getConstructor(MDSApiClient.class);
          MDSApiClient apiClient = apiClientLogIn();
          apiClient.authenticate();
          return (RBACProvider) rbacProviderContructor.newInstance(apiClient);
        default:
          throw new IOException("Unknown access control provided. " + accessControlClassName);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  public BindingsBuilderProvider builder() throws IOException {
    String accessControlClass = config.getAccessControlClassName();

    try {
      if (accessControlClass.equalsIgnoreCase(ACCESS_CONTROL_DEFAULT_CLASS)) {
        return new AclsBindingsBuilder(config);
      } else if (accessControlClass.equalsIgnoreCase(CONFLUENT_CLOUD_CONTROL_CLASS)) {
        return new AclsBindingsBuilder(config);
      } else if (accessControlClass.equalsIgnoreCase(RBAC_ACCESS_CONTROL_CLASS)) {
        MDSApiClient apiClient = apiClientLogIn();
        apiClient.authenticate();
        return new RBACBindingsBuilder(apiClient);
      } else {
        throw new IOException(accessControlClass + " Unknown access control provided.");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private MDSApiClient apiClientLogIn() {
    MDSApiClient apiClient = mdsApiClientBuilder.build();
    String mdsUser = config.getProperty(MDS_USER_CONFIG);
    String mdsPassword = config.getProperty(MDS_PASSWORD_CONFIG);
    apiClient.login(mdsUser, mdsPassword);
    return apiClient;
  }
}
