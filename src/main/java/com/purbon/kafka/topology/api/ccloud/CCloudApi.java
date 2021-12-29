package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.ccloud.requests.KafkaAclRequest;
import com.purbon.kafka.topology.api.ccloud.requests.ServiceAccountRequest;
import com.purbon.kafka.topology.api.ccloud.response.KafkaAclListResponse;
import com.purbon.kafka.topology.api.ccloud.response.ListServiceAccountResponse;
import com.purbon.kafka.topology.api.ccloud.response.ServiceAccountResponse;
import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudApi {

  private static final Logger LOGGER = LogManager.getLogger(CCloudApi.class);

  private JulieHttpClient clusterHttpClient;
  private JulieHttpClient ccloudApiHttpClient;

  private String ccloudApiBaseUrl = "https://api.confluent.cloud";

  public CCloudApi(String baseServerUrl, Configuration config) {
    this(new JulieHttpClient(baseServerUrl, Optional.of(config)), Optional.empty(), config);
  }

  public CCloudApi(
      JulieHttpClient clusterHttpClient,
      Optional<JulieHttpClient> ccloudApiHttpClientOptional,
      Configuration config) {
    this.clusterHttpClient = clusterHttpClient;
    this.ccloudApiHttpClient =
        ccloudApiHttpClientOptional.orElse(
            new JulieHttpClient(ccloudApiBaseUrl, Optional.of(config)));
    this.clusterHttpClient.setBasicAuth(config.getConfluentCloudClusterAuth());
    this.ccloudApiHttpClient.setBasicAuth(config.getConfluentCloudCloudApiAuth());
  }

  public void createAcl(String clusterId, TopologyAclBinding binding) throws IOException {
    String url = String.format("/kafka/v3/clusters/%s/acls", clusterId);
    var request =
        new KafkaAclRequest(binding, String.format("%s%s", clusterHttpClient.baseUrl(), url));
    clusterHttpClient.doPost(url, request.asJson());
  }

  public void deleteAcls(String clusterId, TopologyAclBinding binding) throws IOException {
    String url = String.format("/kafka/v3/clusters/%s/acls", clusterId);
    KafkaAclRequest request =
        new KafkaAclRequest(binding, url);
    clusterHttpClient.doDelete(request.deleteUrl());
  }

  public List<TopologyAclBinding> listAcls(String clusterId) throws IOException {
    String url = String.format("/kafka/v3/clusters/%s/acls", clusterId);

    Response rawResponse = clusterHttpClient.doGet(url);
    KafkaAclListResponse response =
        (KafkaAclListResponse)
            JSON.toObject(rawResponse.getResponseAsString(), KafkaAclListResponse.class);
    return response.getData().stream().map(TopologyAclBinding::new).collect(Collectors.toList());
  }

  public ServiceAccount createServiceAccount(String sa) throws IOException {
    return createServiceAccount(sa, "Managed by JulieOps");
  }

  public void deleteServiceAccount(String sa) throws IOException {
    String url = String.format("/iam/v2/service-accounts/%s", sa);
    ccloudApiHttpClient.doDelete(url);
  }

  public ServiceAccount createServiceAccount(String sa, String description) throws IOException {
    String url = "/iam/v2/service-accounts";
    var request = new ServiceAccountRequest(sa, description);
    String responseBody = ccloudApiHttpClient.doPost(url, request.asJson());

    ServiceAccountResponse response =
        (ServiceAccountResponse) JSON.toObject(responseBody, ServiceAccountResponse.class);
    return new ServiceAccount(
        response.getId(),
        response.getDisplay_name(),
        response.getDescription(),
        response.getMetadata().getResource_name());
  }

  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    String url = "/iam/v2/service-accounts";
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
      finished = nextUrl == null;
      if (!finished) {
        url = nextUrl.replace(ccloudApiBaseUrl, "");
      }
    } while (!finished);

    return accounts;
  }

  private ListServiceAccountResponse getListServiceAccounts(String url) throws IOException {
    Response r = ccloudApiHttpClient.doGet(url);
    return (ListServiceAccountResponse)
        JSON.toObject(r.getResponseAsString(), ListServiceAccountResponse.class);
  }
}
