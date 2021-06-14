package com.purbon.kafka.topology.utils;

public class BasicAuth {
  private final String user;
  private final String password;

  public BasicAuth(String user, String password) {
    this.user = user;
    this.password = password;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}
