package com.purbon.kafka.topology.api;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.JULIE_HTTP_BACKOFF_TIME_MS;
import static com.purbon.kafka.topology.Constants.JULIE_HTTP_RETRY_TIMES;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.utils.PTHttpClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JulieHttpClientTest {

  @Rule public WireMockRule wireMockRule = new WireMockRule(8089);

  private Map<String, String> cliOps;
  private Properties props;

  private PTHttpClient client;

  @Before
  public void before() throws IOException {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();

    props.put(JULIE_HTTP_BACKOFF_TIME_MS, 0);
    Configuration config = new Configuration(cliOps, props);

    client = new PTHttpClient(wireMockRule.baseUrl(), Optional.of(config));
  }

  @Test
  public void shouldResponseFastToNonRetryErrorCodes() throws IOException {
    stubFor(
        get(urlEqualTo("/some/thing"))
            .willReturn(
                aResponse().withHeader("Content-Type", "text/plain").withBody("Hello world!")));
    stubFor(get(urlEqualTo("/some/thing/else")).willReturn(aResponse().withStatus(404)));
    assertThat(client.doGet("/some/thing").getStatus()).isEqualTo(200);
    assertThat(client.doGet("/some/thing/else").getStatus()).isEqualTo(404);
  }

  @Test
  public void shouldRunTheRetryFlowForRetrievableErrorCodes() throws IOException {

    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();

    props.put(JULIE_HTTP_BACKOFF_TIME_MS, 0);
    props.put(JULIE_HTTP_RETRY_TIMES, 5);
    Configuration config = new Configuration(cliOps, props);

    client = new PTHttpClient(wireMockRule.baseUrl(), Optional.of(config));

    stubFor(
        get(urlEqualTo("/some/thing"))
            .inScenario("retrievable")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("retry1"));

    stubFor(
        get(urlEqualTo("/some/thing"))
            .inScenario("retrievable")
            .whenScenarioStateIs("retry1")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("retry2"));

    stubFor(
        get(urlEqualTo("/some/thing"))
            .inScenario("retrievable")
            .whenScenarioStateIs("retry2")
            .willReturn(
                aResponse().withHeader("Content-type", "text/plain").withBody("Hello world!")));

    assertThat(client.doGet("/some/thing").getStatus()).isEqualTo(200);
  }
}
