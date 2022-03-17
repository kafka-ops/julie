package com.purbon.kafka.topology.roles;

import static org.junit.Assert.*;

import org.junit.Test;

public class PrincipalTest {
  @Test
  public void readsUserPrincipal() {
    Principal principal = Principal.fromString("User:sa-foo");
    assertEquals("sa-foo", principal.getServiceAccountName());
    assertEquals(PrincipalType.User, principal.getPrincipalType());
  }

  @Test
  public void readsGroupPrincipal() {
    Principal principal = Principal.fromString("Group:sa-bar");
    assertEquals("sa-bar", principal.getServiceAccountName());
    assertEquals(PrincipalType.Group, principal.getPrincipalType());
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsForInvalidPrincipalTypeString() {
    Principal.fromString("Vulcan:sa-spock");
  }

  @Test
  public void roundTripFromUserPrincipalString() {
    var userPrincipalString = "User:sa-foo";
    assertEquals(userPrincipalString, Principal.fromString(userPrincipalString).toString());
  }

  @Test
  public void roundTripFromGroupPrincipalString() {
    var groupPrincipalString = "Group:sa-bar";
    assertEquals(groupPrincipalString, Principal.fromString(groupPrincipalString).toString());
  }
}
