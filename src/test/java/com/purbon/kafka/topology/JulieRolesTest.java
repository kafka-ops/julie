package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.model.JulieRole;
import com.purbon.kafka.topology.model.JulieRoleAcl;
import com.purbon.kafka.topology.model.JulieRoles;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.serdes.JulieRolesSerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.JinjaUtils;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    JulieRoles roles = parser.deserialise(TestUtils.getResourceFile("/roles-rbac.yaml"));

    assertThat(roles.getRoles()).hasSize(2);
    for (JulieRole role : roles.getRoles()) {
      assertThat(role.getName()).isIn("app", "other");
    }

    JulieRole role = roles.get("app");
    List<String> resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic", "Group", "Subject", "Connector");

    assertThat(role.getName()).isEqualTo("app");
    assertThat(role.getAcls()).hasSize(9);
    assertThat(role.getAcls().get(0).getRole()).isEqualTo("ResourceOwner");

    role = roles.get("other");
    resources =
        role.getAcls().stream().map(JulieRoleAcl::getResourceType).collect(Collectors.toList());
    assertThat(resources).contains("Topic");
    assertThat(role.getName()).isEqualTo("other");
    assertThat(role.getAcls()).hasSize(2);

    TopologySerdes topologySerdes =
        new TopologySerdes(new Configuration(), TopologySerdes.FileType.YAML, new PlanMap());
    Topology topology = topologySerdes.deserialise(TestUtils.getResourceFile("/descriptor.yaml"));

    var project = topology.getProjects().get(0);
    for (Map.Entry<String, List<Other>> entry : project.getOthers().entrySet()) {
      if (!entry.getKey().equals("app")) {
        continue;
      }
      role = roles.get(entry.getKey());
      var other = entry.getValue().get(0);
      var acls =
          role.getAcls().stream()
              .map(
                  acl -> {
                    String resourceName =
                        JinjaUtils.serialise(acl.getResourceName(), other.asMap());
                    return new JulieRoleAcl(
                        acl.getResourceType(),
                        resourceName,
                        acl.getPatternType(),
                        acl.getHost(),
                        acl.getOperation(),
                        acl.getPermissionType());
                  })
              .collect(Collectors.toList());
      var names = acls.stream().map(JulieRoleAcl::getResourceName).collect(Collectors.toList());
      assertThat(names).contains("test.subject", "con");
    }
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
