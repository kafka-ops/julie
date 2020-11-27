package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.DynamicUser;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class KStream extends DynamicUser {

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> applicationId;

  public KStream() {
    this("", new HashMap<>());
  }

  public KStream(
      String principal, HashMap<String, List<String>> topics, Optional<String> applicationId) {
    super(principal, topics);
    this.applicationId = applicationId;
  }

  public KStream(String principal, HashMap<String, List<String>> topics) {
    this(principal, topics, Optional.empty());
  }

  public Optional<String> getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(Optional<String> applicationId) {
    this.applicationId = applicationId;
  }
}
