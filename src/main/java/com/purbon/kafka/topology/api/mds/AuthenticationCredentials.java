package com.purbon.kafka.topology.api.mds;

public class AuthenticationCredentials {

  private final String authToken;
  private final String tokenType;
  private final double expiresIn;

  public AuthenticationCredentials(String authToken, String tokenType, double expriresIn) {
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

  public double getExpiresIn() {
    return expiresIn;
  }
}