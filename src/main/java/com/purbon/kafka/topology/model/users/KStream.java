package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.DynamicUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KStream extends DynamicUser {

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> applicationId;

  @JsonInclude(Include.NON_EMPTY)
  private Optional<Boolean> exactlyOnce;

  public KStream() {
    this("", new HashMap<>());
  }

  public KStream(
      String principal,
      Map<String, List<String>> topics,
      Optional<String> applicationId,
      Optional<Boolean> exactlyOnce) {
    super(principal, topics);
    this.applicationId = applicationId;
    this.exactlyOnce = exactlyOnce;
  }

  public KStream(
      String principal, HashMap<String, List<String>> topics, Optional<String> applicationId) {
    this(principal, topics, applicationId, Optional.of(Boolean.FALSE));
  }

  public KStream(String principal, HashMap<String, List<String>> topics) {
    this(principal, topics, Optional.empty(), Optional.of(Boolean.FALSE));
  }

  public Optional<String> getApplicationId() {
    return applicationId;
  }

  public Optional<Boolean> getExactlyOnce() {
    return exactlyOnce;
  }

  public void setApplicationId(Optional<String> applicationId) {
    this.applicationId = applicationId;
  }

  public void setExactlyOnce(Optional<Boolean> exactlyOnce) {
    this.exactlyOnce = exactlyOnce;
  }
}
