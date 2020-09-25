package com.purbon.kafka.topology.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnvVarToolsTest {
  @Test
  public void underscoresShouldBeReplaced() {
    assertEquals("abc.uvw.xyz", EnvVarTools.envVarNameToPropertyName("abc_uvw_xyz"));
  }
}
