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
  public void roundTripFromString() {
    assertEquals("User:sa-foo", Principal.fromString("User:sa-foo").toString());
  }
}
