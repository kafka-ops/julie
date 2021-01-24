package kafka.ops.topology.model.users.platform;

import kafka.ops.topology.model.User;

public class ControlCenterInstance extends User {

  private String appId;

  public ControlCenterInstance() {
    super("");
  }

  public ControlCenterInstance(String principal, String appId) {
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
