package com.purbon.kafka.topology.model.artefact;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TypeArtefact {
  String name();
}
