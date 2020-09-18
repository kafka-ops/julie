package com.purbon.kafka.topology.serdes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purbon.kafka.topology.model.FlatDescription;
import java.io.File;
import java.io.IOException;

/**
 * Converts a {@link com.purbon.kafka.topology.model.FlatDescription to and from JSON encoded
 * strings.}
 */
public class FlatDescriptionSerde {

  static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
  }

  public static String convertToJsonString(FlatDescription fd) {
    return convertToJsonString(fd, false);
  }

  public static String convertToJsonString(FlatDescription fd, boolean prettyPrint) {
    try {
      if (prettyPrint) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fd);
      } else {
        return mapper.writeValueAsString(fd);
      }
    } catch (JsonProcessingException e) {
      // TODO: add logging
      e.printStackTrace();
    }
    return null;
  }

  public static FlatDescription convertFromJsonString(String json) {
    try {
      return mapper.readValue(json, FlatDescription.class);
    } catch (JsonProcessingException e) {
      // TODO: add logging
      e.printStackTrace();
    }
    return null;
  }

  public static FlatDescription convertFromJsonFile(File file) {
    try {
      return mapper.readValue(file, FlatDescription.class);
    } catch (JsonProcessingException e) {
      // TODO: add logging
      e.printStackTrace();
    } catch (IOException e) {
      // TODO: add proper logging
      e.printStackTrace();
    }
    return null;
  }
}
