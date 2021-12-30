package com.purbon.kafka.topology.api.ccloud;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CCloudApiTest {

  private CCloudApi apiClient;
  private Configuration config;

  @Mock JulieHttpClient httpClient;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void before() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(CCLOUD_CLUSTER_API_KEY, "apiKey");
    props.put(CCLOUD_CLUSTER_API_SECRET, "apiSecret");
    props.put(CCLOUD_CLOUD_API_KEY, "apiKey");
    props.put(CCLOUD_CLOUD_API_SECRET, "apiSecret");
    config = new Configuration(cliOps, props);

    when(httpClient.baseUrl()).thenReturn("http://not.valid:9999");
    apiClient = new CCloudApi(httpClient, Optional.of(httpClient), config);
  }

  @Test
  public void testAclCreateMethod() throws IOException {

    TopologyAclBinding binding =
        new TopologyAclBinding("TOPIC", "foo", "*", "ALL", "User:foo", "LITERAL");

    apiClient.createAcl("clusterId", binding);
    var body =
        "{\"resource_type\":\"TOPIC\","
            + "\"resource_name\":\"foo\","
            + "\"pattern_type\":\"LITERAL\","
            + "\"principal\":\"User:foo\","
            + "\"host\":\"*\","
            + "\"operation\":\"ALL\","
            + "\"permission\":\"ALLOW\"}";

    verify(httpClient, times(1)).doPost("/kafka/v3/clusters/clusterId/acls", body);
  }

  @Test
  public void testAclDeleteMethod() throws IOException {

    TopologyAclBinding binding =
        new TopologyAclBinding("TOPIC", "foo", "*", "ALL", "User:foo", "LITERAL");

    apiClient.deleteAcls("clusterId", binding);
    var url =
        "/kafka/v3/clusters/clusterId/acls?principal=User:foo&pattern_type=LITERAL&resource_type=TOPIC&host=*&permission=ALLOW&resource_name=foo&operation=ALL";
    verify(httpClient, times(1)).doDelete(url);
  }

  @Test
  public void testCreateServiceAccount() throws IOException {

    var principal = "User:foo";

    var url = "/iam/v2/service-accounts";
    var body = "{\"display_name\":\"" + principal + "\",\"description\":\"" + MANAGED_BY + "\"}";
    var createResponse =
        "{\n"
            + "  \"api_version\": \"iam/v2\",\n"
            + "  \"kind\": \"ServiceAccount\",\n"
            + "  \"id\": \"dlz-f3a90de\",\n"
            + "  \"metadata\": {\n"
            + "    \"self\": \"https://api.confluent.cloud/iam/v2/service-accounts/sa-12345\",\n"
            + "    \"resource_name\": \"crn://confluent.cloud/service-account=sa-12345\",\n"
            + "    \"created_at\": \"2006-01-02T15:04:05-07:00\",\n"
            + "    \"updated_at\": \"2006-01-02T15:04:05-07:00\",\n"
            + "    \"deleted_at\": \"2006-01-02T15:04:05-07:00\"\n"
            + "  },\n"
            + "  \"display_name\": \""
            + principal
            + "\",\n"
            + "  \"description\": \""
            + MANAGED_BY
            + "\"\n"
            + "}";

    when(httpClient.doPost(url, body)).thenReturn(createResponse);

    var sa = apiClient.createServiceAccount(principal);
    verify(httpClient, times(1)).doPost(url, body);
    assertThat(sa.getName()).isEqualTo(principal);
    assertThat(sa.getDescription()).isEqualTo(MANAGED_BY);
  }

  @Test
  public void testDeleteServiceAccount() throws IOException {
    var sa = "User:foo";
    apiClient.deleteServiceAccount(sa);
    var url = "/iam/v2/service-accounts/User:foo";
    verify(httpClient, times(1)).doDelete(url);
  }
}
