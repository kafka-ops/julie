package com.purbon.kafka.topology.model.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PrincipalTest {
  @Test
  void readsUserPrincipal() {
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("User:sa-foo");
    assertEquals("sa-foo", principal.getServiceAccountName());
    assertEquals(ConfluentCloudPrincipal.PrincipalType.User, principal.getPrincipalType());
  }

  @Test
  void readsGroupPrincipal() {
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("Group:sa-bar");
    assertEquals("sa-bar", principal.getServiceAccountName());
    assertEquals(ConfluentCloudPrincipal.PrincipalType.Group, principal.getPrincipalType());
  }

  @Test
  void failsForMalformedPrincipalString() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ConfluentCloudPrincipal.fromString("");
        });
  }

  @Test
  void failsForInvalidPrincipalTypeString() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ConfluentCloudPrincipal.fromString("Vulcan:sa-spock");
        });
  }

  @Test
  void roundTripFromUserPrincipalString() {
    var userPrincipalString = "User:sa-foo";
    assertEquals(
        userPrincipalString, ConfluentCloudPrincipal.fromString(userPrincipalString).toString());
  }

  @Test
  void roundTripFromGroupPrincipalString() {
    var groupPrincipalString = "Group:sa-bar";
    assertEquals(
        groupPrincipalString, ConfluentCloudPrincipal.fromString(groupPrincipalString).toString());
  }

  @Test
  void generatesMappedPrincipal() {
    var mappedPrincipal = "User:123456";
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("User:sa-foo");
    assertEquals(mappedPrincipal, principal.toMappedPrincipalString(123456l));
  }
}
