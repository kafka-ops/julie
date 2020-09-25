package com.purbon.kafka.topology.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnvVarTools {
  /**
   * @param prefix
   * @return
   */
  public static Map<String, String> getEnvVarsStartingWith(String prefix) {
    Objects.requireNonNull(prefix);
    final Map<String, String> envVarsWithPrefix = new HashMap<>();
    System.getenv()
        .forEach(
            (envKey, envValue) -> {
              if (envKey.startsWith(prefix)) {
                envVarsWithPrefix.put(envVarNameToPropertyName(envKey), envValue);
              }
            });
    return envVarsWithPrefix;
  }

  /**
   * @param envVarName
   * @return
   */
  public static String envVarNameToPropertyName(String envVarName) {
    return envVarName.toLowerCase().replace('_', '.').replace("...", "_");
  }
}
