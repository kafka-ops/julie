package com.purbon.kafka.topology.clusterstate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.Reader;
import java.io.Writer;

public class YAMLStateProcessor implements StateProcessor {

  static class YAMLStateProcessorException extends RuntimeException {
    public YAMLStateProcessorException(Throwable cause) {
      super(cause);
    }
  }

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // .disable(Feature.WRITE_DOC_START_MARKER)

  @Override
  public ClusterState readState(Reader reader) {
    try {
      return mapper.readValue(reader, ClusterState.class);
    } catch (Exception e) {
      throw new YAMLStateProcessorException(e);
    }
  }

  @Override
  public void writeState(Writer writer, ClusterState clusterState) {
    try {
      mapper.writeValue(writer, clusterState);
    } catch (Exception e) {
      throw new YAMLStateProcessorException(e);
    }
  }
}

/*

aclBindings:
- resourceType:
  resourceName:
  host:
  operation:
  principal:
  pattern:
- resourceType:
  resourceName:
  host:
  operation:
  principal:
  pattern:

 */