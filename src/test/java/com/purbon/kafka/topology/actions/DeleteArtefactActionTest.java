package com.purbon.kafka.topology.actions;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.clients.ArtefactClient;
import com.purbon.kafka.topology.model.artefact.KafkaConnectArtefact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteArtefactActionTest {

  @Mock
  ArtefactClient client;

  @Test
  void shouldComposeDetailedViewOfProperties() {

    var artefact = new KafkaConnectArtefact("path", "label", "name");

    var action = new DeleteArtefactAction(client, artefact);
    var refs = action.refs();
    assertThat(refs).hasSize(1);
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"artefact\" : \"path\",\n"
                + "  \"resource_name\" : \"rn://delete.artefact/com.purbon.kafka.topology.actions.DeleteArtefactAction/name\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.DeleteArtefactAction\"\n"
                + "}");
  }
}
