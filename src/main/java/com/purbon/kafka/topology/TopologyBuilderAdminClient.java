package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Topic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyBuilderAdminClient {

  private static final Logger LOGGER = LogManager.getLogger(TopologyBuilderAdminClient.class);

  private final AdminClient adminClient;

  public TopologyBuilderAdminClient(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public Set<String> listTopics() {
    Set<String> listOfTopics = new HashSet<>();
    try {
      listOfTopics = adminClient
          .listTopics()
          .names()
          .get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
    return listOfTopics;
  }

  public void updateTopicConfig(Topic topic, String fullTopicName) {

    try {
        updateTopicConfigPostAK23(topic, fullTopicName);
    } catch (ExecutionException ex) {
      LOGGER.error(ex);
    }
    catch (InterruptedException ex) {
      LOGGER.error(ex);
    }
  }

  private void updateTopicConfigPreAK23(Topic topic, String fullTopicName)
      throws ExecutionException, InterruptedException {

    Map<ConfigResource, Config> configs = new HashMap<>();

    topic
        .rawConfig()
        .forEach(new BiConsumer<String, String>() {
          @Override
          public void accept(String configKey, String configValue) {
            ConfigResource resource = new ConfigResource(Type.TOPIC, fullTopicName);
            ConfigEntry entry = new ConfigEntry(configKey, configValue);
            Config value = new Config(Collections.singleton(entry));
            configs.put(resource, value);
          }
        });

    adminClient
        .alterConfigs(configs)
        .all()
        .get();
  }

  private void updateTopicConfigPostAK23(Topic topic, String fullTopicName)
      throws ExecutionException, InterruptedException {
    Map<ConfigResource,Collection<AlterConfigOp>> configs = new HashMap<>();

    topic
        .rawConfig()
        .forEach(new BiConsumer<String, String>() {
          @Override
          public void accept(String configKey, String configValue) {
            configs.put(new ConfigResource(Type.TOPIC, fullTopicName),
                Collections
                    .singleton(new AlterConfigOp(new ConfigEntry(configKey, configValue), OpType.SET)));
          }
        });

      adminClient
          .incrementalAlterConfigs(configs)
          .all()
          .get();

  }

  public void createTopic(Topic topic, String fullTopicName) {

    int numPartitions = Integer.parseInt(topic.getConfig().getOrDefault(TopicManager.NUM_PARTITIONS, "3"));
    short replicationFactor = Short.parseShort(topic.getConfig().getOrDefault(TopicManager.REPLICATION_FACTOR, "2"));

    NewTopic newTopic = new NewTopic(fullTopicName, numPartitions, replicationFactor)
        .configs(topic.rawConfig());
    Collection<NewTopic> newTopics = Collections.singleton(newTopic);
    try {
     createAllTopics(newTopics);
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
  }

  private void createAllTopics(Collection<NewTopic> newTopics ) throws ExecutionException, InterruptedException {
    adminClient.createTopics(newTopics)
        .all()
        .get();
  }

  public void deleteTopic(String topic) {
    deleteTopics(Collections.singletonList(topic));
  }

  public void deleteTopics(Collection<String> topics) {
    try {
      adminClient.deleteTopics(topics).all().get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
  }

  /**
   * Find cluster inter protocol version, used to determine the minimum level of Api
   * compatibility
   * @return String, the current Kafka Protocol version
   */
  private String findKafkaVersion() {
    ConfigResource resource = new ConfigResource(Type.BROKER, "inter.broker.protocol.version");
    String kafkaVersion = "";
    try {
      Map<ConfigResource, Config> configs =  adminClient
          .describeConfigs(Collections.singletonList(resource))
          .all()
          .get();
      kafkaVersion = configs.get(resource).get("inter.broker.protocol.version")
          .value()
          .split("-")[0];
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
    return kafkaVersion;
  }

  public void setAclsForProducer(String principal, String topic) {
    List<AclBinding> acls = new ArrayList<>();

    ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
    AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.WRITE, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    createAcls(acls);
  }

  public void setAclsForConsumer(String principal, String topic) {

    List<AclBinding> acls = new ArrayList<>();

    ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
    AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));
    resourcePattern = new ResourcePattern(ResourceType.GROUP, "*", PatternType.LITERAL);
    entry = new AccessControlEntry(principal,"*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    createAcls(acls);
  }

  private void createAcls(Collection<AclBinding> acls) {
    try {
      adminClient
          .createAcls(acls).all().get();
    } catch (InterruptedException e) {
      LOGGER.error(e);
    } catch (ExecutionException e) {
      LOGGER.error(e);
    }
  }


  public void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics) {

    List<AclBinding> acls = new ArrayList<>();

    readTopics.forEach(topic -> {
      ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
      AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.READ, AclPermissionType.ALLOW);
      acls.add(new AclBinding(resourcePattern, entry));
    });

    writeTopics.forEach(topic -> {
      ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
      AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.WRITE, AclPermissionType.ALLOW);
      acls.add(new AclBinding(resourcePattern, entry));
    });

    ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topicPrefix, PatternType.PREFIXED);
    AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.ALL, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    createAcls(acls);

  }
}
