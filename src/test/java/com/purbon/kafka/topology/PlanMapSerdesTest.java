package com.purbon.kafka.topology;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.model.Plan;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.serdes.PlanMapSerdes;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

    Map<String, Object> config = new HashMap<>();
    config.put("foo", "bar");
    config.put("bar", 3);

    Plan plan1 = new Plan("gold", config);
    Plan plan2 = new Plan("silver", config);
    PlanMap map = new PlanMap(Arrays.asList(plan1, plan2));

    String serValue = new PlanMapSerdes().serialise(map);

    assertThat(serValue).contains("name: \"gold\"");
    assertThat(serValue).contains("name: \"silver\"");
    assertThat(serValue).contains("config:");


  }

  @Test
  public void testHappyDeserialization() throws URISyntaxException, IOException {
    URL topologyDescriptor = getClass().getResource("/plans.yaml");
    PlanMap plans = parser.deserialise(Paths.get(topologyDescriptor.toURI()).toFile());

    assertThat(plans.getPlans()).has(new Condition<>(list -> list.size() == 2, "Contains two elements"));
    assertThat(plans.getPlans().get(0).getName()).isEqualTo("gold");

    Map<String, Object> config = new HashMap<>();
    config.put("foo", "bar");
    config.put("bar", 3);

    assertThat(plans.getPlans().get(0).getConfig()).isEqualTo(config);

    assertThat(plans.getPlans().get(1).getName()).isEqualTo("silver");

  }
}
