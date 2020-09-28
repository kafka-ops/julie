package com.purbon.kafka.topology.utils;

@FunctionalInterface
public interface CheckedFunction<T, R> {
  R apply(T t) throws Exception;
}
