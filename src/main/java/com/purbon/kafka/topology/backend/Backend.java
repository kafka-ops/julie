package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import java.io.IOException;

public interface Backend {

  default void configure(Configuration config) {
    // empty if not implemented
  }

  void createOrOpen();

  void createOrOpen(BackendController.Mode mode);

  void close();

  void save(BackendState state) throws IOException;

  BackendState load() throws IOException;
}
