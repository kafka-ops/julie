package com.purbon.kafka.topology.model.artefact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KsqlTableArtefactTest {

  @Test
  void labelNotPrintedWhenMissing() {
    KsqlTableArtefact artefact = new KsqlTableArtefact("file.ksql", null, "TABLE_NAME");
    assertEquals("TABLE TABLE_NAME", artefact.toString());
  }

  @Test
  void labelPrintedWhenPresent() {
    KsqlTableArtefact artefact = new KsqlTableArtefact("file.ksql", "zone-a", "TABLE_NAME");
    assertEquals("TABLE TABLE_NAME (zone-a)", artefact.toString());
  }
}
