package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.model.cluster.ServiceAccount;

import java.io.IOException;
import java.util.Map;

public interface CCloud {
    Map<String, ServiceAccount> serviceAccounts() throws IOException;

    ServiceAccount newServiceAccount(String name, String description) throws IOException;

    void deleteServiceAccount(int id) throws IOException;
}
