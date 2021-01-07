package com.purbon.kafka.topology.api.ccloud.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties
public class ServiceAccountsResponse {

    @JsonProperty("users")
    private List<ServiceAccount> users;

    public List<ServiceAccount> getUsers() {
        return users;
    }

    public void setUsers(List<ServiceAccount> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "ServiceAccountsResponse{" +
                "users=" + users +
                '}';
    }
}
