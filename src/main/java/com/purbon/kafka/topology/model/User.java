package com.purbon.kafka.topology.model;

public class User {

  private String principal;

  public User() {
    this("");
  }

  public User(String principal) {
    this.principal = principal;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }
}
