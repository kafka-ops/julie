package com.purbon.kafka.topology.api.ccloud;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.CCLOUD_API_KEY;
import static com.purbon.kafka.topology.Constants.CCLOUD_API_SECRET;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    props.put(CCLOUD_API_KEY, "apiKey");
    props.put(CCLOUD_API_SECRET, "apiSecret");

    config = new Configuration(cliOps, props);

    when(httpClient.baseUrl()).thenReturn("http://not.valid:9999");
    apiClient = new CCloudApi(httpClient, config);
  }

  @Test
  public void testAclCreateMethod() throws IOException {

    TopologyAclBinding binding =
        new TopologyAclBinding("TOPIC", "foo", "*", "ALL", "User:foo", "LITERAL");

    apiClient.createAcl("clusterId", binding);
    var body =
        "{\"principal\":\"User:foo\","
            + "\"metadata\":{\"self\":\"http://not.valid:9999/kafka/v3/clusters/clusterId/acls?principal=User:foo&pattern_type=LITERAL&kind=KafkaAcl&resource_type=TOPIC&host=*&permission=ALLOW&resource_name=foo&operation=ALL\"},"
            + "\"pattern_type\":\"LITERAL\","
            + "\"kind\":\"KafkaAcl\","
            + "\"resource_type\":\"TOPIC\","
            + "\"host\":\"*\",\"permission\":\"ALLOW\","
            + "\"resource_name\":\"foo\","
            + "\"operation\":\"ALL\"}";
    verify(httpClient, times(1)).doPost("/kafka/v3/clusters/clusterId/acls", body);
  }

  @Test
  public void testAclDeleteMethod() throws IOException {

    TopologyAclBinding binding =
        new TopologyAclBinding("TOPIC", "foo", "*", "ALL", "User:foo", "LITERAL");

    apiClient.deleteAcls("clusterId", binding);
    var url =
        "http://not.valid:9999/kafka/v3/clusters/clusterId/acls?principal=User:foo&pattern_type=LITERAL&resource_type=TOPIC&host=*&permission=ALLOW&resource_name=foo&operation=ALL";
    verify(httpClient, times(1)).doDelete(url);
  }

  @Test
  public void testCreateServiceAccount() throws IOException {
    var sa = "User:foo";
    apiClient.createServiceAccount(sa);
    var url = "https://api.confluent.cloud/iam/v2/service-accounts";
    var body = "{\"description\":\"Managed by JulieOps\",\"display_name\":\"User:foo\"}";
    verify(httpClient, times(1)).doPost(url, body);
  }

  @Test
  public void testDeleteServiceAccount() throws IOException {
    var sa = "User:foo";
    apiClient.deleteServiceAccount(sa);
    var url = "https://api.confluent.cloud/iam/v2/service-accounts/User:foo";
    verify(httpClient, times(1)).doDelete(url);
  }
}
