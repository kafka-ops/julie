package com.purbon.kafka.topology.api.ccloud;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.*;
import static com.purbon.kafka.topology.api.ccloud.CCloudApi.V2_IAM_SERVICE_ACCOUNTS_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

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
  public void before() throws IOException {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();
    props.put(CCLOUD_CLUSTER_API_KEY, "apiKey");
    props.put(CCLOUD_CLUSTER_API_SECRET, "apiSecret");
    props.put(CCLOUD_CLOUD_API_KEY, "apiKey");
    props.put(CCLOUD_CLOUD_API_SECRET, "apiSecret");
    props.put(CCLOUD_SA_ACCOUNT_QUERY_PAGE_SIZE, 1);
    config = new Configuration(cliOps, props);

    apiClient = new CCloudApi(httpClient, Optional.of(httpClient), config);
  }

  @Test
  public void testAclCreateMethod() throws IOException {
    when(httpClient.baseUrl()).thenReturn("http://not.valid:9999");

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

  @Test
  public void listServiceAccountsShouldAcceptPage() throws IOException {

    String body01 = "{\n" +
            "  \"api_version\": \"iam/v2\",\n" +
            "  \"kind\": \"ServiceAccountList\",\n" +
            "  \"metadata\": {\n" +
            "    \"first\": \"https://api.confluent.cloud/iam/v2/service-accounts\",\n" +
            "    \"last\": \"https://api.confluent.cloud/iam/v2/service-accounts?page_token=bcAOehAY8F16YD84Z1wT\",\n" +
            "    \"prev\": \"https://api.confluent.cloud/iam/v2/service-accounts?page_token=YIXRY97wWYmwzrax4dld\",\n" +
            "    \"next\": \"https://api.confluent.cloud/iam/v2/service-accounts?page_token=UvmDWOB1iwfAIBPj6EYb\",\n" +
            "    \"total_size\": 123\n" +
            "  },\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"api_version\": \"iam/v2\",\n" +
            "      \"kind\": \"ServiceAccount\",\n" +
            "      \"id\": \"dlz-f3a90de\",\n" +
            "      \"metadata\": {\n" +
            "        \"self\": \"https://api.confluent.cloud/iam/v2/service-accounts/sa-12345\",\n" +
            "        \"resource_name\": \"crn://confluent.cloud/service-account=sa-12345\",\n" +
            "        \"created_at\": \"2006-01-02T15:04:05-07:00\",\n" +
            "        \"updated_at\": \"2006-01-02T15:04:05-07:00\",\n" +
            "        \"deleted_at\": \"2006-01-02T15:04:05-07:00\"\n" +
            "      },\n" +
            "      \"display_name\": \"DeLorean_auto_repair\",\n" +
            "      \"description\": \"Doc's repair bot for the DeLorean\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    Response response01 = new Response(null, 200, body01);

    when(httpClient.doGet(String.format("%s?page_size=%d", V2_IAM_SERVICE_ACCOUNTS_URL, 1)))
            .thenReturn(response01);

    String body02 = "{\n" +
            "  \"api_version\": \"iam/v2\",\n" +
            "  \"kind\": \"ServiceAccountList\",\n" +
            "  \"metadata\": {\n" +
            "    \"first\": \"https://api.confluent.cloud/iam/v2/service-accounts\",\n" +
            "    \"last\": \"https://api.confluent.cloud/iam/v2/service-accounts?page_token=bcAOehAY8F16YD84Z1wT\",\n" +
            "    \"prev\": \"https://api.confluent.cloud/iam/v2/service-accounts?page_token=YIXRY97wWYmwzrax4dld\",\n" +
            "    \"total_size\": 123\n" +
            "  },\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"api_version\": \"iam/v2\",\n" +
            "      \"kind\": \"ServiceAccount\",\n" +
            "      \"id\": \"abc-f3a90de\",\n" +
            "      \"metadata\": {\n" +
            "        \"self\": \"https://api.confluent.cloud/iam/v2/service-accounts/sa-12345\",\n" +
            "        \"resource_name\": \"crn://confluent.cloud/service-account=sa-12345\",\n" +
            "        \"created_at\": \"2006-01-02T15:04:05-07:00\",\n" +
            "        \"updated_at\": \"2006-01-02T15:04:05-07:00\",\n" +
            "        \"deleted_at\": \"2006-01-02T15:04:05-07:00\"\n" +
            "      },\n" +
            "      \"display_name\": \"MacFly\",\n" +
            "      \"description\": \"Doc's repair bot for the MacFly\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    Response response02 = new Response(null, 200, body02);


    when(httpClient.doGet("/iam/v2/service-accounts?page_token=UvmDWOB1iwfAIBPj6EYb"))
            .thenReturn(response02);

    Set<ServiceAccount> accounts = apiClient.listServiceAccounts();
    assertThat(accounts).hasSize(2);
  }
}
