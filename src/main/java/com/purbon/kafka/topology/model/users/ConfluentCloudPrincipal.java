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
    String type = "User";
    String name;
    if (principalString.contains(COLON_AS_SEPARATOR)) {
      String[] user = principalString.split(COLON_AS_SEPARATOR);
      type = user[0];
      name = user[1];
    } else {
      name = principalString;
    }
    return new ConfluentCloudPrincipal(PrincipalType.valueOf(type), name);

  }

  @Override
  public String toString() {
    return String.format("%s%s%s", principalType, COLON_AS_SEPARATOR, serviceAccountName);
  }

  public String toMappedPrincipalString(long serviceAccountNumericId) {
    return String.format("%s%s%d", principalType, COLON_AS_SEPARATOR, serviceAccountNumericId);
  }
}
