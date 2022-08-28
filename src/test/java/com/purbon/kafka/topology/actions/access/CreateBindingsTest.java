package com.purbon.kafka.topology.actions.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.AccessControlProvider;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.HashSet;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateBindingsTest {

  @Mock AccessControlProvider provider;

  @Test
  void shouldComposeDetailedViewOfProperties() {

    var bindings = new HashSet<TopologyAclBinding>();
    bindings.add(
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(), "Topic", "host", "op", "principal", "LITERAL"));
    bindings.add(
        TopologyAclBinding.build(
            ResourceType.CLUSTER.name(),
            "resourceName",
            "host",
            "operation",
            "service",
            "pattern"));
    var action = new CreateBindings(provider, bindings);
    var refs = action.refs();
    assertThat(refs).hasSize(2);
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"acl.pattern\" : \"pattern\",\n"
                + "  \"acl.resource_type\" : \"CLUSTER\",\n"
                + "  \"acl.operation\" : \"operation\",\n"
                + "  \"acl.resource_name\" : \"resourceName\",\n"
                + "  \"resource_name\" : \"rn://create.binding/com.purbon.kafka.topology.actions.access.CreateBindings/CLUSTER/resourceName/service/operation/pattern\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccessControlAction$1\",\n"
                + "  \"acl.principal\" : \"service\"\n"
                + "}");
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"acl.pattern\" : \"LITERAL\",\n"
                + "  \"acl.resource_type\" : \"CLUSTER\",\n"
                + "  \"acl.operation\" : \"op\",\n"
                + "  \"acl.resource_name\" : \"Topic\",\n"
                + "  \"resource_name\" : \"rn://create.binding/com.purbon.kafka.topology.actions.access.CreateBindings/CLUSTER/Topic/principal/op/LITERAL\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccessControlAction$1\",\n"
                + "  \"acl.principal\" : \"principal\"\n"
                + "}");
  }
}
