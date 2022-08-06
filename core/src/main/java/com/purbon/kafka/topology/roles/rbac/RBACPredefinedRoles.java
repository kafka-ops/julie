package com.purbon.kafka.topology.roles.rbac;

public class RBACPredefinedRoles {

  public static final String DEVELOPER_READ = "DeveloperRead";
  public static final String DEVELOPER_WRITE = "DeveloperWrite";
  public static final String DEVELOPER_MANAGE = "DeveloperManage";

  public static final String RESOURCE_OWNER = "ResourceOwner";

  public static final String SECURITY_ADMIN = "SecurityAdmin";
  public static final String SYSTEM_ADMIN = "SystemAdmin";

  public static boolean isClusterScopedRole(String role) {
    return !DEVELOPER_READ.equals(role)
        && !DEVELOPER_WRITE.equals(role)
        && !DEVELOPER_MANAGE.equals(role)
        && !RESOURCE_OWNER.equals(role);
  }
}
