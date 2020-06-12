package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;

public class ControlCenter extends User {

  private String appId;

  public ControlCenter() {
    super("");
  }

  public ControlCenter(String principal, String appId) {
    super(principal);
    this.appId = appId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
