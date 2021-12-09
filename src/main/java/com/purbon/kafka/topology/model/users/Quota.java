package com.purbon.kafka.topology.model.users;

import java.util.Optional;

public class Quota {

  private String principal;
  private Optional<Double> producer_byte_rate;
  private Optional<Double> consumer_byte_rate;
  private Optional<Double> request_percentage;

  public Quota() {
    this.principal = null;
    producer_byte_rate = Optional.empty();
    consumer_byte_rate = Optional.empty();
    request_percentage = Optional.empty();
  }

  public Quota(
      String principal,
      Optional<Double> producer_byte_rate,
      Optional<Double> consumer_byte_rate,
      Optional<Double> request_percentage) {
    this();
    this.principal = principal;
    this.producer_byte_rate = producer_byte_rate;
    this.consumer_byte_rate = consumer_byte_rate;
    this.request_percentage = request_percentage;
  }

  public Quota(
      String principal, Optional<Double> producer_byte_rate, Optional<Double> consumer_byte_rate) {
    this();
    this.principal = principal;
    this.producer_byte_rate = producer_byte_rate;
    this.consumer_byte_rate = consumer_byte_rate;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public Optional<Double> getProducer_byte_rate() {
    return producer_byte_rate;
  }

  public void setProducer_byte_rate(Optional<Double> producer_byte_rate) {
    this.producer_byte_rate = producer_byte_rate;
  }

  public Optional<Double> getConsumer_byte_rate() {
    return consumer_byte_rate;
  }

  public void setConsumer_byte_rate(Optional<Double> consumer_byte_rate) {
    this.consumer_byte_rate = consumer_byte_rate;
  }

  public Optional<Double> getRequest_percentage() {
    return request_percentage;
  }

  public void setRequest_percentage(Optional<Double> request_percentage) {
    this.request_percentage = request_percentage;
  }
}
