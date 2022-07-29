package com.purbon.kafka.topology.utils;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils<T> {

  private final Stream<T> stream;

  public StreamUtils(Stream<T> stream) {
    this.stream = stream;
  }

  public Set<T> filterAsSet(Predicate<T> p) {
    return stream.filter(p).collect(Collectors.toSet());
  }
}
