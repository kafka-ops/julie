package server.api.model.topology.users;

import server.api.model.topology.User;

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
