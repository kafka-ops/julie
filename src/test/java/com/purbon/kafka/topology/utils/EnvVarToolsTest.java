package com.purbon.kafka.topology.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EnvVarToolsTest {
  @Test
  public void underscoresShouldBeReplaced() {
    assertEquals("uvw.xyz", EnvVarTools.envVarNameToPropertyName("abc_uvw_xyz", "abc"));
  }

  @Test
  public void tripleUnderscoresShouldConvertToSingle() {
    assertEquals("cde_ghi.klm", EnvVarTools.envVarNameToPropertyName("abc_cde___ghi.klm", "abc"));
  }

  @Test
  public void filteringPref() {
    final String prefix = "ABC";
    final Map<String, String> input = new HashMap<>();
    input.put(prefix + "_XYZ_YES", "xyz_yes_val");
    input.put(prefix + "_XYZ_XXX", "xyz_xxx_val");
    input.put("OTHER", "otherval");

    final Map<String, String> expectedOutput = new HashMap<>();
    expectedOutput.put("xyz.yes", "xyz_yes_val");
    expectedOutput.put("xyz.xxx", "xyz_xxx_val");

    assertEquals(expectedOutput, EnvVarTools.filterAndMapEnvironment(input, prefix));
  }
}
