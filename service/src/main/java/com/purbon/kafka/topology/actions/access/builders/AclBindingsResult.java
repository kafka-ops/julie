package com.purbon.kafka.topology.actions.access.builders;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.Collection;

public final class AclBindingsResult {

  private Collection<TopologyAclBinding> aclBindings;
  private String errorMessage;

  private AclBindingsResult(Collection<TopologyAclBinding> aclBindings, String errorMessage) {
    this.aclBindings = aclBindings;
    this.errorMessage = errorMessage;
  }

  public static AclBindingsResult forError(String errorMessage) {
    return new AclBindingsResult(null, errorMessage);
  }

  public static AclBindingsResult forAclBindings(Collection<TopologyAclBinding> aclBindings) {
    return new AclBindingsResult(aclBindings, null);
  }

  public boolean isError() {
    return errorMessage != null;
  }

  public Collection<TopologyAclBinding> getAclBindings() {
    return aclBindings;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
