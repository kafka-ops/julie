package com.purbon.kafka.topology.model.artefact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KsqlStreamArtefactTest {

  @Test
  void labelNotPrintedWhenMissing() {
    KsqlStreamArtefact artefact = new KsqlStreamArtefact("file.ksql", null, "STREAM_NAME");
    assertEquals("STREAM STREAM_NAME", artefact.toString());
  }

  @Test
  void labelPrintedWhenPresent() {
    KsqlStreamArtefact artefact = new KsqlStreamArtefact("file.ksql", "zone-a", "STREAM_NAME");
    assertEquals("STREAM STREAM_NAME (zone-a)", artefact.toString());
  }
}
