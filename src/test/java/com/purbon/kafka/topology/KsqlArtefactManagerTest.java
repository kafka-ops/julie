package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.CommandLineInterface.CLIENT_CONFIG_OPTION;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.api.ksql.KsqlApiClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import com.purbon.kafka.topology.utils.TestUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KsqlArtefactManagerTest {

  Configuration config;

  @Mock KsqlApiClient client;

  TopologySerdes parser;

  @BeforeEach
  public void before() {

    Map<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    cliOps.put(CLIENT_CONFIG_OPTION, "/fooBar");

    Properties props = new Properties();

    config = new Configuration(cliOps, props);

    parser = new TopologySerdes(config, new PlanMap());
  }

  @AfterEach
  public void after() {}

  @Test
  void testArtefactGenerationOrder() {
    String topologyFileName = "/descriptor-ksql-multiple.yaml";
    String topologyFilePath = TestUtils.getResourceFilename(topologyFileName);

    Topology topology = parser.deserialise(TestUtils.getResourceFile(topologyFileName));

    KSqlArtefactManager m = new KSqlArtefactManager(client, config, topologyFilePath);

    var artefacts = m.parseNewArtefacts(topology);

    assertThat(artefacts).hasSize(4);
    var ksqlArtefacts = topology.getProjects().get(0).getKsqlArtefacts();
    int i = 0;
    for (Artefact artefact : artefacts) {
      if (i < 2) {
        assertThat(artefact).isEqualTo(ksqlArtefacts.getStreams().get(i));
      } else {
        assertThat(artefact).isEqualTo(ksqlArtefacts.getTables().get(i - 2));
      }
      i++;
    }
  }

  @Test
  void testArtefactsForDeletionOrder() {
    String topologyFileName = "/descriptor-ksql-multiple.yaml";
    String topologyFilePath = TestUtils.getResourceFilename(topologyFileName);

    Topology topology = parser.deserialise(TestUtils.getResourceFile(topologyFileName));

    KSqlArtefactManager m = new KSqlArtefactManager(client, config, topologyFilePath);

    var artefacts = m.parseNewArtefacts(topology);

    System.out.println(artefacts);

    var toDelete = m.findArtefactsToBeDeleted(artefacts, Collections.emptySet());
    System.out.println(toDelete);

    assertThat(toDelete).hasSize(4);

    int j = toDelete.size() - 1;
    for (Artefact artefact : artefacts) {
      assertThat(artefact).isEqualTo(toDelete.get(j));
      j--;
    }
  }
}
