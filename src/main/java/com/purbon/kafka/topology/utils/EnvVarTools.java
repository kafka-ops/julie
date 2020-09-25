package com.purbon.kafka.topology.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnvVarTools {
  /**
   * Get all environment variables whose name starts with a given prefix.
   *
   * @param prefix a non-null String
   * @return all environment variables starting with prefix
   */
  public static Map<String, String> getEnvVarsStartingWith(String prefix) {
    Objects.requireNonNull(prefix);
    final Map<String, String> envVarsWithPrefix = new HashMap<>();
    System.getenv()
        .forEach(
            (envKey, envValue) -> {
              if (envKey.startsWith(prefix)) {
                envVarsWithPrefix.put(envVarNameToPropertyName(envKey, prefix), envValue);
              }
            });
    return envVarsWithPrefix;
  }

  /**
   * Converts a environment variable name to a corresponding property name.
   *
   * <p>Conversion happens according to the following rules:
   *
   * <p>- drop prefix and a single separator character - convert to lower-case - replace every '_'
   * with a '.', can be escaped by using "___", which will be replaced by a single '_'
   *
   * @param envVarName
   * @param prefixToDrop
   * @return
   */
  public static String envVarNameToPropertyName(String envVarName, String prefixToDrop) {
    final int dropLength = prefixToDrop.length() + 1;
    return envVarName.substring(dropLength).toLowerCase().replace('_', '.').replace("...", "_");
  }
}
