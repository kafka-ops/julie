package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class ServiceAccountResponse {

    @JsonProperty("user")
    private ServiceAccount user;

    public ServiceAccount getUser() {
        return user;
    }

    public void setUser(ServiceAccount user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "ServiceAccountResponse{" +
                "user=" + user +
                '}';
    }
}
