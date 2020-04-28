package com.purbon.kafka.topology.clusterstate;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.List;

public class ClusterState {

  private List<TopologyAclBinding> aclBindings;
  // TODO private List<TopologyRBACBinding> rbacBindings;

  public ClusterState() {}

  public ClusterState(List<TopologyAclBinding> aclBindings) {
    this.aclBindings = aclBindings;
  }

  public List<TopologyAclBinding> getAclBindings() {
    return aclBindings;
  }

  public void setAclBindings(List<TopologyAclBinding> aclBindings) {
    this.aclBindings = aclBindings;
  }
}
