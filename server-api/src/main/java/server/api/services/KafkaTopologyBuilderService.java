package server.api.services;

import static com.purbon.kafka.topology.BuilderCLI.ADMIN_CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.ALLOW_DELETE_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.BROKERS_OPTION;
import static com.purbon.kafka.topology.BuilderCLI.QUIET_OPTION;

import com.purbon.kafka.topology.AccessControlProviderFactory;
import com.purbon.kafka.topology.KafkaTopologyBuilder;
import com.purbon.kafka.topology.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.TopologyBuilderAdminClientBuilder;
import com.purbon.kafka.topology.TopologyBuilderConfig;
import com.purbon.kafka.topology.api.mds.MDSApiClientBuilder;

import com.purbon.kafka.topology.model.Topology;
import io.micronaut.context.annotation.Value;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import server.api.models.TopologyDeco;

@Singleton
public class KafkaTopologyBuilderService {

  @Value("${micronaut.topology_builder.brokers_list}")
  private String brokersList;
  @Value("${micronaut.topology_builder.allow_delete}")
  private boolean allowDelete;
  @Value("${micronaut.topology_builder.admin_config_file}")
  private String adminClientConfigFile;

  public void sync(String topologyFile) throws IOException {
    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config());
    TopologyBuilderAdminClient adminClient =
        new TopologyBuilderAdminClientBuilder(builderConfig).build();
    AccessControlProviderFactory accessControlProviderFactory =
        new AccessControlProviderFactory(
            builderConfig, adminClient, new MDSApiClientBuilder(builderConfig));

    KafkaTopologyBuilder builder =
        new KafkaTopologyBuilder(
            topologyFile, builderConfig, adminClient, accessControlProviderFactory.get());

    try {
      builder.run();
    } finally {
      adminClient.close();
    }
  }

  public void sync(Topology topology) throws IOException {
    TopologyBuilderConfig builderConfig = new TopologyBuilderConfig(config());

    KafkaTopologyBuilder builder;

    if (topology instanceof TopologyDeco) {
      builder = new KafkaTopologyBuilder(((TopologyDeco)topology).asTopology(), builderConfig);
    } else {
      builder = new KafkaTopologyBuilder(topology, builderConfig);
    }

    try {
      builder.run();
    } finally {
      builder.close();
    }
  }

  private Map<String, String> config() {
    Map<String, String> config = new HashMap<>();
    config.put(BROKERS_OPTION, brokersList);
    config.put(ALLOW_DELETE_OPTION, String.valueOf(allowDelete));
    config.put(QUIET_OPTION, "true");
    config.put(ADMIN_CLIENT_CONFIG_OPTION, adminClientConfigFile);
    return config;
  }

}
