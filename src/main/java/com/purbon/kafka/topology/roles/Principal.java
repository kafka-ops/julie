package com.purbon.kafka.topology.roles;

import lombok.Value;

@Value
public class Principal {
  public static final String COLON_AS_SEPARATOR = ":";
  PrincipalType principalType;
  String serviceAccountName;

  public static Principal fromString(String principalString) {
    String[] user = principalString.split(COLON_AS_SEPARATOR);
    return new Principal(PrincipalType.valueOf(user[0]), user[1]);
  }

  public String getMappedPrincipal(long serviceAccountNumericId) {
    return String.format("%s%s%d", principalType, COLON_AS_SEPARATOR, serviceAccountNumericId);
  }

  @Override
  public String toString() {
    return String.format("%s%s%s", principalType, COLON_AS_SEPARATOR, serviceAccountName);
  }
}
