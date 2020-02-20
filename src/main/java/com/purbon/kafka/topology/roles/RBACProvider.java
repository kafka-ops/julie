package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlProvider;
import java.util.Collection;
import java.util.List;

public class RBACProvider implements AccessControlProvider {

  @Override
  public void clearAcls() {

  }

  @Override
  public void setAclsForConnect(String principal, String topicPrefix, List<String> readTopics,
      List<String> writeTopics) {

  }

  @Override
  public void setAclsForStreamsApp(String principal, String topicPrefix, List<String> readTopics,
      List<String> writeTopics) {

  }

  @Override
  public void setAclsForConsumers(Collection<String> principals, String topic) {

  }

  @Override
  public void setAclsForProducers(Collection<String> principals, String topic) {

  }
}
