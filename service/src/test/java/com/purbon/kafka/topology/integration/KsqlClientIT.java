package com.purbon.kafka.topology.integration;

import static com.purbon.kafka.topology.api.ksql.KsqlApiClient.STREAM_TYPE;
import static com.purbon.kafka.topology.api.ksql.KsqlApiClient.TABLE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.api.ksql.KsqlClientConfig;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.KsqlContainer;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import java.io.IOException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KsqlClientIT {

  static SaslPlaintextKafkaContainer container;
  static KsqlContainer ksqlContainer;

  @After
  public void after() {
    ksqlContainer.stop();
    container.stop();
  }

  @Before
  public void configure() {
    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    ksqlContainer = new KsqlContainer(container);
    ksqlContainer.start();
  }

  @Test
  public void testStreamTableCreateAndDelete() throws IOException {

    KsqlApiClient client =
        new KsqlApiClient(KsqlClientConfig.builder().setServer(ksqlContainer.getUrl()).build());

    String streamName = "riderLocations";

    String sql =
        "CREATE STREAM "
            + streamName
            + " (profileId VARCHAR, latitude DOUBLE, longitude DOUBLE)\n"
            + "  WITH (kafka_topic='locations', value_format='json', partitions=1);";

    client.add(sql);

    List<String> queries = client.list();
    assertThat(queries).hasSize(1);

    client.delete(streamName, STREAM_TYPE);

    queries = client.list();
    assertThat(queries).hasSize(0);

    String tableName = "users";
    sql =
        "CREATE TABLE "
            + tableName
            + " (\n"
            + "     id BIGINT PRIMARY KEY,\n"
            + "     usertimestamp BIGINT,\n"
            + "     gender VARCHAR,\n"
            + "     region_id VARCHAR\n"
            + "   ) WITH (\n"
            + "     KAFKA_TOPIC = 'my-users-topic', \n"
            + "     KEY_FORMAT='KAFKA', PARTITIONS=2, REPLICAS=1,"
            + "     VALUE_FORMAT = 'JSON'\n"
            + "   );";

    client.add(sql);

    queries = client.list();
    assertThat(queries).hasSize(1);

    client.delete(tableName, TABLE_TYPE);

    queries = client.list();
    assertThat(queries).hasSize(0);
  }
}
