package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.ccloud.requests.KafkaAclRequest;
import com.purbon.kafka.topology.api.ccloud.requests.ServiceAccountRequest;
import com.purbon.kafka.topology.api.ccloud.response.ListServiceAccountResponse;
import com.purbon.kafka.topology.api.ccloud.response.ServiceAccountResponse;
import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

  public ServiceAccount createServiceAccount(String sa) throws IOException {
    return createServiceAccount(sa, "Managed by JulieOps");
  }

  public void deleteServiceAccount(String sa) throws IOException {
    String url = String.format("https://api.confluent.cloud/iam/v2/service-accounts/%s", sa);
    httpClient.doDelete(url);
  }

  public ServiceAccount createServiceAccount(String sa, String description) throws IOException {
    String url = "https://api.confluent.cloud/iam/v2/service-accounts";
    var request = new ServiceAccountRequest(sa, description);
    String responseBody = httpClient.doPost(url, request.asJson());

    ServiceAccountResponse response =
        (ServiceAccountResponse) JSON.toObject(responseBody, ServiceAccountResponse.class);
    return new ServiceAccount(
        response.getId(),
        response.getDisplay_name(),
        response.getDescription(),
        response.getMetadata().getResource_name());
  }

  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    String url = "https://api.confluent.cloud/iam/v2/service-accounts";
    boolean finished;
    Set<ServiceAccount> accounts = new HashSet<>();

    do {
      ListServiceAccountResponse response = getListServiceAccounts(url);
      for (ServiceAccountResponse serviceAccountResponse : response.getData()) {
        var resourceId = serviceAccountResponse.getMetadata().getResource_name();
        var serviceAccount =
            new ServiceAccount(
                serviceAccountResponse.getId(),
                serviceAccountResponse.getDisplay_name(),
                serviceAccountResponse.getDescription(),
                resourceId);
        accounts.add(serviceAccount);
      }

      String nextUrl = response.getMetadata().getNext();
      String lastUrl = response.getMetadata().getLast();
      finished = nextUrl.equals(lastUrl);
    } while (!finished);

    return accounts;
  }

  private ListServiceAccountResponse getListServiceAccounts(String url) throws IOException {
    Response r = httpClient.doGet(url);
    return (ListServiceAccountResponse)
        JSON.toObject(r.getResponseAsString(), ListServiceAccountResponse.class);
  }
}
