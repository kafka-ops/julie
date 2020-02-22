package com.purbon.kafka.topology;

import java.util.Collection;
import java.util.List;

public interface AccessControlProvider {

  void clearAcls();
  void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);
  void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics, List<String> writeTopics);
  void setAclsForConsumers(Collection<String> principals, String topic);
  void setAclsForProducers(Collection<String> principals, String topic);

  default void setPredefinedRole(String principal, String predefinedRole, String topicPrefix) {
   //NOOP
  }
}
