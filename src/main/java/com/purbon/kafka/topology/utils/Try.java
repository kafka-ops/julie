package com.purbon.kafka.topology.utils;

public class Try<Exception, R> {

  private final Exception failure;
  private final R succes;

  public Try(Exception failure, R succes) {
    this.failure = failure;
    this.succes = succes;
  }
}
