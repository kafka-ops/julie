package com.purbon.kafka.topology.api.mds;

public class AuthenticationCredentials {

  private final String authToken;
  private final String tokenType;
  private final Integer expiresIn;

  public AuthenticationCredentials(String authToken, String tokenType, Integer expiresIn) {
    this.authToken = authToken;
    this.tokenType = tokenType;
    this.expiresIn = expiresIn;
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
