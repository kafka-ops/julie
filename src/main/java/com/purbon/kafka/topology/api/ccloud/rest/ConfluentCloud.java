package com.purbon.kafka.topology.api.ccloud.rest;

import com.purbon.kafka.topology.api.ccloud.rest.json.AuthRequest;
import com.purbon.kafka.topology.api.ccloud.rest.json.AuthSuccessResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ConfluentCloud {

    private ClientConfig clientConfig;


    public ConfluentCloud(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void login() throws IOException {

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(clientConfig.getEmail());
        authRequest.setPassword(clientConfig.getPassword());


        String body = clientConfig.getObjectMapper().writeValueAsString(authRequest);

        String response = RestClient.execute(Request.Post(clientConfig.getBaseUrl()+"sessions")
                .bodyString(body, ContentType.APPLICATION_JSON))
                .asString();

        AuthSuccessResponse authSuccessResponse = clientConfig.getObjectMapper().readValue(response, AuthSuccessResponse.class);
        clientConfig.setToken(authSuccessResponse.getToken());
    }

    public ServiceAccounts serviceAccounts() {
        return new ServiceAccounts(clientConfig);
    }
}
