package com.purbon.kafka.topology.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JSONUtilsTest {

  @BeforeEach
  public void setup() {}

  @Test
  void toMapDeserialization() throws JsonProcessingException {

    String jsonString = "{\n" + "\t\"foo\": 2,\n" + "\t\"test\": \"boo\"\n" + "}";

    Map<String, Object> jsonAsMap = JSON.toMap(jsonString);

    assertEquals(2, jsonAsMap.get("foo"));
    assertEquals("boo", jsonAsMap.get("test"));
  }

  @Test
  void toStringSerialization() throws JsonProcessingException {

    String expectJSONAsString = "{\"test\":\"2\",\"foo\":\"bar\"}";
    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    map.put("test", "2");

    String jsonAsString = JSON.asString(map);

    assertEquals(expectJSONAsString, jsonAsString);
  }

  @Test
  void toArrayDeserialisation() throws JsonProcessingException {

    String jsonAsString = " [\n \t\"1\", \"2\", \"3\"\n ]";

    List<String> jsonAsList = JSON.toArray(jsonAsString);
    assertEquals("1", jsonAsList.get(0));
    assertEquals(ArrayList.class, jsonAsList.getClass());
  }
}
