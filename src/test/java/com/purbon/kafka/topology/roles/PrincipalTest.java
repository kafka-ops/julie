package com.purbon.kafka.topology.roles;

import static org.junit.Assert.*;

import org.junit.Test;

public class PrincipalTest {
  @Test
  public void readsUserPrincipal() {
    Principal principal = Principal.fromString("User:sa-foo");
    assertEquals("sa-foo", principal.serviceAccountName);
    assertEquals(PrincipalType.User, principal.principalType);
  }

  @Test
  public void readsGroupPrincipal() {
    Principal principal = Principal.fromString("Group:sa-bar");
    assertEquals("sa-bar", principal.serviceAccountName);
    assertEquals(PrincipalType.Group, principal.principalType);
  }

  @Test
  public void roundTripFromUserString() {
    var userPrincipalString = "User:sa-foo";
    assertEquals(userPrincipalString, Principal.fromString(userPrincipalString).toString());
  }

  @Test
  public void roundTripFromGroupString() {
    assertEquals("User:sa-bar", Principal.fromString("User:sa-bar").toString());
  }
}
