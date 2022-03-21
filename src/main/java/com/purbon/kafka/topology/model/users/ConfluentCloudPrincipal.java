package com.purbon.kafka.topology.model.users;

import lombok.Value;

@Value
public class ConfluentCloudPrincipal {
  public static final String COLON_AS_SEPARATOR = ":";

  public enum PrincipalType {
    User,
    Group
  }

  private PrincipalType principalType;
  private String serviceAccountName;

  public static ConfluentCloudPrincipal fromString(String principalString) {
    String[] user = principalString.split(COLON_AS_SEPARATOR);
    return new ConfluentCloudPrincipal(PrincipalType.valueOf(user[0]), user[1]);
  }

  @Override
  public String toString() {
    return String.format("%s%s%s", principalType, COLON_AS_SEPARATOR, serviceAccountName);
  }

  public String getMappedPrincipal(long serviceAccountNumericId) {
    return String.format("%s%s%d", principalType, COLON_AS_SEPARATOR, serviceAccountNumericId);
  }
}
