package com.purbon.kafka.topology.api.ksql;

import io.confluent.ksql.api.client.ExecuteStatementResult;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class QueryResponse {

  public static final String QUERY_ID = "queryID";

  private Optional<String> queryId;

  public QueryResponse(Optional<String> queryId) {
    this.queryId = queryId;
  }

  public QueryResponse(ExecuteStatementResult result) {
    this(result.queryId());
  }

  public Optional<String> getQueryId() {
    return queryId;
  }

  public Map<String, Object> asMap() {
    return Collections.singletonMap(QUERY_ID, queryId);
  }
}
