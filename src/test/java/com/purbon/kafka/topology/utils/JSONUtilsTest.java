package com.purbon.kafka.topology.utils;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class JSONUtilsTest {

  @Before
  public void setup() {}

  @Test
  public void testToMapDeserialization() throws JsonProcessingException {

    String jsonString = "{\n" + "\t\"foo\": 2,\n" + "\t\"test\": \"boo\"\n" + "}";

    Map<String, Object> jsonAsMap = JSON.toMap(jsonString);

    assertEquals(2, jsonAsMap.get("foo"));
    assertEquals("boo", jsonAsMap.get("test"));
  }

  @Test
  public void testToStringSerialization() throws JsonProcessingException {

    String expectJSONAsString = "{\"test\":\"2\",\"foo\":\"bar\"}";
    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    map.put("test", "2");

    String jsonAsString = JSON.asString(map);

    assertEquals(expectJSONAsString, jsonAsString);
  }

  @Test
  public void testToArrayDeserialisation() throws JsonProcessingException {

    String jsonAsString = " [\n \t\"1\", \"2\", \"3\"\n ]";

    List<String> jsonAsList = JSON.toArray(jsonAsString);
    assertEquals("1", jsonAsList.get(0));
    assertEquals(ArrayList.class, jsonAsList.getClass());
  }
}
