package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.api.ccloud.rest.ClientConfig;
import com.purbon.kafka.topology.api.ccloud.rest.ConfluentCloud;
import com.purbon.kafka.topology.api.ccloud.rest.json.ServiceAccount;

import java.io.IOException;

public class Test {

    public static void main(String... args) throws IOException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setEmail("blah@blah.com");
        clientConfig.setPassword("blahblahblahblah");
        ConfluentCloud confluentCloud = new ConfluentCloud(clientConfig);
        confluentCloud.login();
        System.out.println(confluentCloud.serviceAccounts().list());

        ServiceAccount serviceAccount = confluentCloud.serviceAccounts().create("Testttiiiiiing", "Desc");

        confluentCloud.serviceAccounts().delete(serviceAccount.getId());
    }

}
