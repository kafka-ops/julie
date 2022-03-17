package com.purbon.kafka.topology.roles;

import lombok.Value;

@Value
public class Principal {
  PrincipalType principalType;
  String serviceAccountName;

  public static Principal fromString(String principalString) {
    String[] user = principalString.split(":");
    return new Principal(PrincipalType.valueOf(user[0]), user[1]);
  }

  @Override
  public String toString() {
    return String.format("%s:%s", principalType, serviceAccountName);
  }
}
