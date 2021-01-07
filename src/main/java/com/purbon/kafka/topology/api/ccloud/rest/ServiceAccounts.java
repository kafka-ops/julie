package com.purbon.kafka.topology.api.ccloud.rest;


import com.purbon.kafka.topology.api.ccloud.rest.json.*;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.List;

public class ServiceAccounts {

    private ClientConfig clientConfig;

    ServiceAccounts(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }


    public List<ServiceAccount> list() throws IOException {

        String response = RestClient.execute(Request.Get(clientConfig.getBaseUrl()+"service_accounts"))
                .asString();

        return clientConfig.getObjectMapper().readValue(response, ServiceAccountsResponse.class).getUsers();
    }

    public ServiceAccount create(String name, String description) throws IOException {
        ServiceAccountCreateRequest serviceAccount = new ServiceAccountCreateRequest(name, description);
        ServiceAccountCreateRequestWrapper wrapper = new ServiceAccountCreateRequestWrapper(serviceAccount);

        String body = clientConfig.getObjectMapper().writeValueAsString(wrapper);

        String response = RestClient.execute(Request.Post(clientConfig.getBaseUrl()+"service_accounts")
                .bodyString(body, ContentType.APPLICATION_JSON))
                .asString();

        return clientConfig.getObjectMapper().readValue(response, ServiceAccountResponse.class).getUser();
    }

    public void delete(int id) throws IOException {
        ServiceAccountDeleteRequest serviceAccount = new ServiceAccountDeleteRequest(id);
        ServiceAccountDeleteRequestWrapper wrapper = new ServiceAccountDeleteRequestWrapper(serviceAccount);

        String body = clientConfig.getObjectMapper().writeValueAsString(wrapper);

        RestClient.execute(RestClient.DeleteBody(clientConfig.getBaseUrl()+"service_accounts")
                .bodyString(body, ContentType.APPLICATION_JSON))
                .asString();
    }

}
