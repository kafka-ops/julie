package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.api.mds.RequestScope;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestScopeDeser extends StdDeserializer<RequestScope> {

  public RequestScopeDeser() {
    this(null);
  }

  protected RequestScopeDeser(Class<?> vc) {
    super(vc);
  }

  @Override
  public RequestScope deserialize(JsonParser parser, DeserializationContext context)
      throws IOException, JsonProcessingException {
    RequestScope scope = new RequestScope();
    JsonNode rootNode = parser.getCodec().readTree(parser);

    JsonNode clusters = rootNode.get("clusters");
    Map<String, String> requestClusters = new HashMap<>();
    clusters
        .fields()
        .forEachRemaining(entry -> requestClusters.put(entry.getKey(), entry.getValue().asText()));
    scope.setClusters(Collections.singletonMap("clusters", requestClusters));

    JsonNode resources = rootNode.get("resources");
    for (int i = 0; i < resources.size(); i++) {
      JsonNode resource = resources.get(i);
      String name = resource.get("name").asText();
      String patternType = resource.get("patternType").asText();
      String resourceType = resource.get("resourceType").asText();
      scope.addResource(resourceType, name, patternType);
    }
    scope.build();

    return scope;
  }
}
