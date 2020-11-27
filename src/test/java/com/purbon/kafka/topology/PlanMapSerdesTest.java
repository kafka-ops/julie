package com.purbon.kafka.topology;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.model.Plan;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.serdes.PlanMapSerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

public class PlanMapSerdesTest {

  PlanMapSerdes parser;

  @Before
  public void before() {
    parser = new PlanMapSerdes();
  }

  @Test
  public void testSerialization() throws JsonProcessingException {
    Map<String, String> config = new HashMap<>();
    config.put("foo", "bar");
    config.put("bar", "3");

    Plan plan1 = new Plan("gold", config);
    Plan plan2 = new Plan("silver", config);

    Map<String, Plan> plans = new HashMap<>();
    plans.put("gold", plan1);
    plans.put("silver", plan2);

    PlanMap map = new PlanMap(plans);

    String serValue = new PlanMapSerdes().serialise(map);

    assertThat(serValue).contains("alias: \"gold\"");
    assertThat(serValue).contains("alias: \"silver\"");
    assertThat(serValue).contains("config:");
  }

  @Test
  public void testHappyDeserialization() throws IOException {
    PlanMap plans = parser.deserialise(TestUtils.getResourceFile("/plans.yaml"));

    assertThat(plans).has(new Condition<>(list -> list.size() == 2, "Contains two elements"));
    assertThat(plans.get("gold").getAlias()).isEqualTo("gold");

    Map<String, String> config = new HashMap<>();
    config.put("foo", "bar");
    config.put("bar", "3");

    assertThat(plans.get("gold").getConfig()).isEqualTo(config);

    assertThat(plans.get("silver").getAlias()).isEqualTo("silver");
  }
}
