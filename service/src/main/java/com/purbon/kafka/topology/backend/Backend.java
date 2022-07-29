package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import java.io.IOException;

public interface Backend {

  default void configure(Configuration config) {
    // empty if not implemented
  }

  default void createOrOpen() {
    // empty if not implemented
  }

  default void createOrOpen(BackendController.Mode mode) {
    // empty if not implemented
  }

  void close();

  void save(BackendState state) throws IOException;

  BackendState load() throws IOException;
}
