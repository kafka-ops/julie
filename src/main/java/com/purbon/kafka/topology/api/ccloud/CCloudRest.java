package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.api.ccloud.rest.ClientConfig;
import com.purbon.kafka.topology.api.ccloud.rest.ConfluentCloud;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class CCloudRest implements CCloud {

    ConfluentCloud confluentCloud;

    public CCloudRest(String baseUrl, String email, String password) throws IOException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setBaseUrl(baseUrl);
        clientConfig.setEmail(email);
        clientConfig.setPassword(password);
        confluentCloud = new ConfluentCloud(clientConfig);
        confluentCloud.login();
    }

    @Override
    public Map<String, ServiceAccount> serviceAccounts() throws IOException {
        return confluentCloud.serviceAccounts().list().stream().collect(Collectors.toMap(this::name, this::convert));
    }

    @Override
    public ServiceAccount newServiceAccount(String name, String description) throws IOException {
        return convert(confluentCloud.serviceAccounts().create(name, description));
    }

    @Override
    public void deleteServiceAccount(int id) throws IOException {
        confluentCloud.serviceAccounts().delete(id);
    }


    private String name(com.purbon.kafka.topology.api.ccloud.rest.json.ServiceAccount serviceAccount) {
        return serviceAccount.getName();
    }
    private ServiceAccount convert(com.purbon.kafka.topology.api.ccloud.rest.json.ServiceAccount serviceAccount) {
        return new ServiceAccount(serviceAccount.getId(), serviceAccount.getName(), serviceAccount.getDescription());
    }
}
