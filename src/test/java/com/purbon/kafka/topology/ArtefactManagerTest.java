package com.purbon.kafka.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import com.purbon.kafka.topology.utils.TestUtils;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ArtefactManagerTest {

  public class MyArtefactManager extends ArtefactManager {

    public MyArtefactManager(
        ArtefactClient client, Configuration config, String topologyFileOrDir) {
      super(client, config, topologyFileOrDir);
    }

    public MyArtefactManager(
        Map<String, ? extends ArtefactClient> clients,
        Configuration config,
        String topologyFileOrDir) {
      super(clients, config, topologyFileOrDir);
    }

    @Override
    Collection<? extends Artefact> loadActualClusterStateIfAvailable(ExecutionPlan plan) {
      return new ArrayList<>();
    }

    @Override
    Set<? extends Artefact> parseNewArtefacts(Topology topology) {
      return new HashSet<>();
    }

    @Override
    boolean isAllowDelete() {
      return false;
    }

    @Override
    String rootPath() {
      return "";
    }

    @Override
    public void printCurrentState(PrintStream out) {}
  }

  @Mock public ArtefactClient mockClient1;

  @Mock public ArtefactClient mockClient2;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  public void testClientSelection() {

    Map<String, ArtefactClient> clients = new HashMap<>();
    clients.put("server0", mockClient1);
    clients.put("server1", mockClient2);

    String file = TestUtils.getResourceFilename("/descriptor.yaml");

    MyArtefactManager artefactManager = new MyArtefactManager(clients, new Configuration(), file);

    Artefact server0Artefact = new KafkaConnectArtefact("/path", "server0", "foo");
    ArtefactClient selectedClient = artefactManager.selectClient(server0Artefact);
    assertThat(selectedClient).isEqualTo(mockClient1);

    Artefact server1Artefact = new KafkaConnectArtefact("/path", "server1", "foo");
    ArtefactClient selectedClient1 = artefactManager.selectClient(server1Artefact);
    assertThat(selectedClient1).isEqualTo(mockClient2);

    Artefact serverBarArtefact = new KafkaConnectArtefact("/path", "bar", "foo");
    ArtefactClient selectedClientBar = artefactManager.selectClient(serverBarArtefact);
    assertThat(selectedClientBar).isNull();
  }
}
