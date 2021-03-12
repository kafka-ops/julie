package com.purbon.kafka.topology.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Utils {

  public static Stream<String> asNullableStream(List<String> items) {
    Optional<List<String>> optional = Optional.ofNullable(items);
    return optional.stream().flatMap(Collection::stream);
  }
}
