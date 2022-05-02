package com.purbon.kafka.topology.utils;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.clients.JulieHttpClient;

import java.io.IOException;
import java.util.Optional;

public class PTHttpClient extends JulieHttpClient {

    public PTHttpClient(String server, Optional<Configuration> config) throws IOException {
        super(server, config);
    }
}
