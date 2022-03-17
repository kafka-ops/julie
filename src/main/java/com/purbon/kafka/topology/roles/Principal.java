package com.purbon.kafka.topology.roles;

import lombok.Value;
import lombok.val;

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
    val COLON = ':';
    return String.format("%s%c%s", principalType, COLON, serviceAccountName);
  }
}
