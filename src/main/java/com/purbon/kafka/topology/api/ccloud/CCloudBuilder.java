package com.purbon.kafka.topology.api.ccloud;

import com.purbon.kafka.topology.TopologyBuilderConfig;

import java.io.IOException;

public class CCloudBuilder {

    private TopologyBuilderConfig topologyBuilderConfig;

    public CCloudBuilder(TopologyBuilderConfig topologyBuilderConfig) {
        this.topologyBuilderConfig = topologyBuilderConfig;
    }

    public CCloud build() throws IOException {
        if (topologyBuilderConfig.enabledConfluentCloud()) {
            String env = topologyBuilderConfig.getConfluentCloudEnv();
            if (topologyBuilderConfig.enabledConfluentCloudRest()) {
                String baseUrl = topologyBuilderConfig.getConfluentCloudUrl();
                String email = topologyBuilderConfig.getConfluentCloudEmail();
                String password = topologyBuilderConfig.getConfluentCloudPassword();
                return new CCloudRest(baseUrl, email, password);
            } else {
                return new CCloudCLI(env);
            }
        }
        return null;
    }
}
