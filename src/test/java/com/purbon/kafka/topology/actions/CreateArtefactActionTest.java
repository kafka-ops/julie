package com.purbon.kafka.topology.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.Artefact;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateArtefactActionTest {

  @Mock ArtefactClient client;

  @Test
  void shouldComposeDetailedViewOfProperties() {

    var artefacts = new ArrayList<Artefact>();
    var artefact = new KafkaConnectArtefact("path", "label", "name");

    var action = new CreateArtefactAction(client, "/foo/bar", artefacts, artefact);
    var refs = action.refs();
    assertThat(refs).hasSize(1);
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"artefact\" : \"path\",\n"
                + "  \"resource_name\" : \"rn://create.artefact/com.purbon.kafka.topology.actions.CreateArtefactAction/name\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.CreateArtefactAction\"\n"
                + "}");
  }
}
