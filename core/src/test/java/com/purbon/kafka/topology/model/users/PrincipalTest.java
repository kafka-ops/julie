package com.purbon.kafka.topology.model.users;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PrincipalTest {
  @Test
  public void readsUserPrincipal() {
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("User:sa-foo");
    assertEquals("sa-foo", principal.getServiceAccountName());
    assertEquals(ConfluentCloudPrincipal.PrincipalType.User, principal.getPrincipalType());
  }

  @Test
  public void readsGroupPrincipal() {
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("Group:sa-bar");
    assertEquals("sa-bar", principal.getServiceAccountName());
    assertEquals(ConfluentCloudPrincipal.PrincipalType.Group, principal.getPrincipalType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsForMalformedPrincipalString() {
    ConfluentCloudPrincipal.fromString("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsForInvalidPrincipalTypeString() {
    ConfluentCloudPrincipal.fromString("Vulcan:sa-spock");
  }

  @Test
  public void roundTripFromUserPrincipalString() {
    var userPrincipalString = "User:sa-foo";
    assertEquals(
        userPrincipalString, ConfluentCloudPrincipal.fromString(userPrincipalString).toString());
  }

  @Test
  public void roundTripFromGroupPrincipalString() {
    var groupPrincipalString = "Group:sa-bar";
    assertEquals(
        groupPrincipalString, ConfluentCloudPrincipal.fromString(groupPrincipalString).toString());
  }

  @Test
  public void generatesMappedPrincipal() {
    var mappedPrincipal = "User:123456";
    ConfluentCloudPrincipal principal = ConfluentCloudPrincipal.fromString("User:sa-foo");
    assertEquals(mappedPrincipal, principal.toMappedPrincipalString(123456l));
  }
}
