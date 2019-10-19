package com.purbon.kafka.topology;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ACLManager {

  private final KafkaAdminClient adminClient;

  public ACLManager(KafkaAdminClient adminClient) {
    this.adminClient = adminClient;
  }

  public void deleteAclsForPrincipals(List<String> principals) {
    throw new NotImplementedException();
  }
  public void setAclsForConsumers(List<String> principals, String topic) {
    principals.forEach(principal -> setAclsForConsumer(principal, topic));
  }

  public void setAclsForProducers(List<String> principals, String topic) {
    principals.forEach(principal -> setAclsForProducer(principal, topic));
  }

  private void setAclsForProducer(String principal, String topic) {
    List<AclBinding> acls = new ArrayList<>();

    ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
    AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.WRITE, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    adminClient
        .createAcls(acls);
  }

  private void setAclsForConsumer(String principal, String topic) {

    List<AclBinding> acls = new ArrayList<>();

    ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topic, PatternType.LITERAL);
    AccessControlEntry entry = new AccessControlEntry(principal,"*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));
    resourcePattern = new ResourcePattern(ResourceType.GROUP, "*", PatternType.LITERAL);
    entry = new AccessControlEntry(principal,"*", AclOperation.READ, AclPermissionType.ALLOW);
    acls.add(new AclBinding(resourcePattern, entry));

    adminClient
        .createAcls(acls);
  }
}
