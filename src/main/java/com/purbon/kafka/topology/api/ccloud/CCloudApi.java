package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudApi {

  private static final Logger LOGGER = LogManager.getLogger(CCloudApi.class);

  private JulieHttpClient httpClient;

  public CCloudApi(String baseServerUrl, Configuration config) {
    this(new JulieHttpClient(baseServerUrl, Optional.of(config)), config);
  }

  public CCloudApi(JulieHttpClient httpClient, Configuration config) {
    this.httpClient = httpClient;
    var basicAuth = config.getConfluentCloudAuth();
    this.httpClient.setBasicAuth(basicAuth);
  }

  public void createAcl(String clusterId, TopologyAclBinding binding) throws IOException {
    String url = String.format("/kafka/v3/clusters/%s/acls", clusterId);
    var request = new KafkaAclRequest(binding, String.format("%s%s", httpClient.baseUrl(), url));
    httpClient.doPost(url, request.asJson());
  }

  public void deleteAcls(String clusterId, TopologyAclBinding binding) throws IOException {
    String url = String.format("%s/kafka/v3/clusters/%s/acls", httpClient.baseUrl(), clusterId);
    KafkaAclRequest request = new KafkaAclRequest(binding, url);
    httpClient.doDelete(request.deleteUrl());
  }

  public void createServiceAccount(String sa) throws IOException {
    String url = "https://api.confluent.cloud/iam/v2/service-accounts";
    var request = new ServiceAccountRequest(sa);
    httpClient.doPost(url, request.asJson());
  }

  public void deleteServiceAccount(String sa) throws IOException {
    String url = String.format("https://api.confluent.cloud/iam/v2/service-accounts/%s", sa);
    httpClient.doDelete(url);
  }
}
