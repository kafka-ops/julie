package com.purbon.kafka.topology.roles;

public class Principal {
  final String serviceAccountName;
  final PrincipalType principalType;

  public Principal(PrincipalType principalType, String serviceAccountName) {
    this.principalType = principalType;
    this.serviceAccountName = serviceAccountName;
  }

  public static Principal fromString(String principalString) {
    String[] user = principalString.split(":");
    return new Principal(PrincipalType.valueOf(user[0]), user[1]);
  }

  @Override
  public String toString() {
    return String.format("%s:%s", principalType, serviceAccountName);
  }
}
