package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.model.JulieRole;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.JulieRoles;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.JulieRolesSerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JulieRolesTest {

  JulieRolesSerdes parser;

  @Before
  public void before() {
    this.parser = new JulieRolesSerdes();
  }

  @After
  public void after() {}

  @Test
  public void testSerdes() throws IOException {
    JulieRoles roles = parser.deserialise(TestUtils.getResourceFile("/roles.yaml"));

    assertThat(roles.getRoles()).hasSize(2);
    for (JulieRole role : roles.getRoles()) {
      assertThat(role.getName()).isIn("app", "other");
    }

    JulieRole role = roles.get("app");
    List<String> resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic", "Group");
    assertThat(role.getName()).isEqualTo("app");
    assertThat(role.getAcls()).hasSize(4);
    assertThat(role.getAcls().get(0).getRole()).isEqualTo("ALL");

    role = roles.get("other");
    resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic");
    assertThat(role.getName()).isEqualTo("other");
    assertThat(role.getAcls()).hasSize(2);
  }

  @Test(expected = IOException.class)
  public void testTopologyValidationException() throws IOException {
    JulieRoles roles = parser.deserialise(TestUtils.getResourceFile("/roles.yaml"));
    TopologySerdes topologySerdes = new TopologySerdes();

    Topology topology = topologySerdes.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    roles.validateTopology(topology);
  }

  @Test
  public void testTopologyValidationCorrect() throws IOException {
    JulieRoles roles = parser.deserialise(TestUtils.getResourceFile("/roles-goodTest.yaml"));
    TopologySerdes topologySerdes = new TopologySerdes();

    Topology topology = topologySerdes.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));
    roles.validateTopology(topology);
  }
}
