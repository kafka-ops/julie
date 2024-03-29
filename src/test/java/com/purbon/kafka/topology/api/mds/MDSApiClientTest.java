package com.purbon.kafka.topology.api.mds;

import static com.purbon.kafka.topology.roles.rbac.RBACPredefinedRoles.DEVELOPER_READ;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class MDSApiClientTest {

  private MDSApiClient apiClient;

  @Before
  public void before() throws IOException {
    apiClient = new MDSApiClient("http://not_used:8090");
  }

  @Test
  public void testBindSubjectRole() {
    String principal = "User:foo";
    String subject = "topic-value";

    TopologyAclBinding binding =
        apiClient
            .bind(principal, DEVELOPER_READ)
            .forSchemaSubject(subject)
            .apply("Subject", subject);

    MDSRequest mdsRequest = apiClient.buildRequest(binding);

    assertThat(mdsRequest.getUrl()).isEqualTo("User:foo/roles/DeveloperRead/bindings");
    assertThat(mdsRequest.getJsonEntity())
        .isEqualTo(
            "{\"resourcePatterns\":[{\"name\":\"topic-value\",\"patternType\":\"LITERAL\",\"resourceType\":\"Subject\"}],\"scope\":{\"clusters\":{\"kafka-cluster\":\"\",\"schema-registry-cluster\":\"\"}}}");
  }

  @Test
  public void testBindSubjectRoleWithoutResourceType() {
    String principal = "User:foo";
    String subject = "topic-value";

    TopologyAclBinding binding =
        apiClient.bind(principal, DEVELOPER_READ).forSchemaSubject(subject).apply();

    MDSRequest mdsRequest = apiClient.buildRequest(binding);

    assertThat(mdsRequest.getUrl()).isEqualTo("User:foo/roles/DeveloperRead/bindings");
    assertThat(mdsRequest.getJsonEntity())
        .isEqualTo(
            "{\"resourcePatterns\":[{\"name\":\"topic-value\",\"patternType\":\"LITERAL\",\"resourceType\":\"Subject\"}],\"scope\":{\"clusters\":{\"kafka-cluster\":\"\",\"schema-registry-cluster\":\"\"}}}");
  }

  @Test
  public void testBindConnectRole() {
    String principal = "User:foo";
    String connectorName = "jdbc-sink";

    TopologyAclBinding binding =
        apiClient
            .bind(principal, DEVELOPER_READ)
            .forAKafkaConnector(connectorName)
            .apply("Connector", connectorName);

    MDSRequest mdsRequest = apiClient.buildRequest(binding);

    assertThat(mdsRequest.getUrl()).isEqualTo("User:foo/roles/DeveloperRead/bindings");
    assertThat(mdsRequest.getJsonEntity())
        .isEqualTo(
            "{\"resourcePatterns\":[{\"name\":\"jdbc-sink\",\"patternType\":\"LITERAL\",\"resourceType\":\"Connector\"}],\"scope\":{\"clusters\":{\"kafka-cluster\":\"\",\"connect-cluster\":\"\"}}}");
  }
}
