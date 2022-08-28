package com.purbon.kafka.topology.model.artefact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KsqlTableArtefactTest {

  @Test
  void testLabelNotPrintedWhenMissing() {
    KsqlTableArtefact artefact = new KsqlTableArtefact("file.ksql", null, "TABLE_NAME");
    assertEquals("TABLE TABLE_NAME", artefact.toString());
  }

  @Test
  void testLabelPrintedWhenPresent() {
    KsqlTableArtefact artefact = new KsqlTableArtefact("file.ksql", "zone-a", "TABLE_NAME");
    assertEquals("TABLE TABLE_NAME (zone-a)", artefact.toString());
  }
}
