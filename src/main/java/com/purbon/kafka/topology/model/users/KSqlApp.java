package com.purbon.kafka.topology.model.users;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.purbon.kafka.topology.model.DynamicUser;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class KSqlApp extends DynamicUser {

  @JsonInclude(Include.NON_EMPTY)
  private Optional<String> applicationId;

  private String ksqlDbId;

  public KSqlApp() {
    this("", new HashMap<>());
  }

  public KSqlApp(
      String principal, HashMap<String, List<String>> topics, Optional<String> applicationId) {
    super(principal, topics);
    this.applicationId = applicationId;
  }

  public KSqlApp(String principal, HashMap<String, List<String>> topics) {
    this(principal, topics, Optional.empty());
  }

  public Optional<String> getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(Optional<String> applicationId) {
    this.applicationId = applicationId;
  }

  public void setKsqlDbId(String ksqlDbId) {
    this.ksqlDbId = ksqlDbId;
  }

  public String getKsqlDbId() {
    return ksqlDbId;
  }
}
