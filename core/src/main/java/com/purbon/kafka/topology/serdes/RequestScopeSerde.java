package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.purbon.kafka.topology.api.mds.RequestScope;
import java.io.IOException;
import java.util.Map;

public class RequestScopeSerde extends StdSerializer<RequestScope> {

  public RequestScopeSerde() {
    this(null);
  }

  protected RequestScopeSerde(Class<RequestScope> t) {
    super(t);
  }

  @Override
  public void serialize(RequestScope request, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    Map<String, Map<String, String>> scope = request.getScope();
    for (String key : scope.keySet()) {
      gen.writeObjectField(key, scope.get(key));
    }
    gen.writeObjectField("resources", request.getResources());
    gen.writeEndObject();
  }
}
