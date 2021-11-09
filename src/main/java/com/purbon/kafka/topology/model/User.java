package com.purbon.kafka.topology.model;

import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class User {

  private String principal;
  private Map<String, String> metadata;

  public User(String principal) {
    this.principal = principal;
  }
}
