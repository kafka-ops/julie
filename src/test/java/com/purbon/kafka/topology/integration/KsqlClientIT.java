package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.KsqlContainer;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class KsqlClientIT {

    static SaslPlaintextKafkaContainer container;
    static KsqlContainer ksqlContainer;

    @After
    public void after() {
        ksqlContainer.stop();
        container.stop();
    }

    @Before
    public void configure() throws InterruptedException {
        container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
        container.start();
        ksqlContainer = new KsqlContainer(container);
        ksqlContainer.start();
        Thread.sleep(3000);
    }

    @Test
    public void testStreamRegistration() throws IOException {

        KsqlApiClient client = new KsqlApiClient(ksqlContainer.getHost(), ksqlContainer.getPort());

        String sql = "CREATE STREAM riderLocations (profileId VARCHAR, latitude DOUBLE, longitude DOUBLE)\n" +
                "  WITH (kafka_topic='locations', value_format='json', partitions=1);";

        client.add(sql);

       List<String> queries = client.list();
       assertThat(queries).hasSize(2);
    }

}
