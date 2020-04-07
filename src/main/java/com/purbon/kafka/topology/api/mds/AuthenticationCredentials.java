package com.purbon.kafka.topology.api.mds;

public class AuthenticationCredentials {

  private final String authToken;
  private final String tokenType;
  private final Integer expiresIn;

  public AuthenticationCredentials(String authToken, String tokenType, Integer expriresIn) {
    this.authToken = authToken;
    this.tokenType = tokenType;
    this.expiresIn = expriresIn;
  }

  public String getAuthToken() {
    return authToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public Integer getExpiresIn() {
    return expiresIn;
  }
}
