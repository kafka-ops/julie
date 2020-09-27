package com.purbon.kafka.topology.utils;

import com.hubspot.jinjava.Jinjava;
import java.util.Map;

public class JinjaUtils {

  private static Jinjava jinjava = new Jinjava();

  public static String serialise(String format, Map<String, Object> context) {
    return jinjava.render(format, context);
  }
}
