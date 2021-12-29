package com.purbon.kafka.topology.api.ccloud;

import static com.purbon.kafka.topology.Constants.MANAGED_BY;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CCloudApi {

  private static final Logger LOGGER = LogManager.getLogger(CCloudApi.class);

  private static final String V2_IAM_SERVICE_ACCOUNTS_URL = "/iam/v2/service-accounts";
  private static final String V3_KAFKA_CLUSTER_URL = "/kafka/v3/clusters/";

  private JulieHttpClient clusterHttpClient;
  private JulieHttpClient ccloudApiHttpClient;

  private String ccloudApiBaseUrl = "https://api.confluent.cloud";
  private static final String V3_KAFKA_CLUSTER_ACL_PATTERN = V3_KAFKA_CLUSTER_URL + "%s/acls";

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
    String url = String.format(V3_KAFKA_CLUSTER_ACL_PATTERN, clusterId);
    var request =
        new KafkaAclRequest(binding, String.format("%s%s", clusterHttpClient.baseUrl(), url));
    clusterHttpClient.doPost(url, JSON.asString(request));
  }

  public void deleteAcls(String clusterId, TopologyAclBinding binding) throws IOException {
    String url = String.format(V3_KAFKA_CLUSTER_ACL_PATTERN, clusterId);
    KafkaAclRequest request = new KafkaAclRequest(binding, url);
    clusterHttpClient.doDelete(request.deleteUrl());
  }

  public List<TopologyAclBinding> listAcls(String clusterId) throws IOException {
    String url = String.format(V3_KAFKA_CLUSTER_ACL_PATTERN, clusterId);
    List<TopologyAclBinding> acls = new ArrayList<>();
    do {
      Response rawResponse = clusterHttpClient.doGet(url);
      KafkaAclListResponse response =
          (KafkaAclListResponse)
              JSON.toObject(rawResponse.getResponseAsString(), KafkaAclListResponse.class);
      acls.addAll(
          response.getData().stream().map(TopologyAclBinding::new).collect(Collectors.toList()));
      url = response.getMetadata().getNext();
    } while (url != null);
    return acls;
  }

  public ServiceAccount createServiceAccount(String sa) throws IOException {
    return createServiceAccount(sa, MANAGED_BY);
  }

  public void deleteServiceAccount(String sa) throws IOException {
    String url = String.format("%s/%s", V2_IAM_SERVICE_ACCOUNTS_URL, sa);
    ccloudApiHttpClient.doDelete(url);
  }

  public ServiceAccount createServiceAccount(String sa, String description) throws IOException {
    var request = new ServiceAccountRequest(sa, description);
    var requestJson = JSON.asString(request);
    LOGGER.debug("createServiceAccount request=" + requestJson);
    String responseBody = ccloudApiHttpClient.doPost(V2_IAM_SERVICE_ACCOUNTS_URL, requestJson);

    ServiceAccountResponse response =
        (ServiceAccountResponse) JSON.toObject(responseBody, ServiceAccountResponse.class);
    return new ServiceAccount(
        response.getId(),
        response.getDisplay_name(),
        response.getDescription(),
        response.getMetadata().getResource_name());
  }

  public Set<ServiceAccount> listServiceAccounts() throws IOException {
    String url = V2_IAM_SERVICE_ACCOUNTS_URL;
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
