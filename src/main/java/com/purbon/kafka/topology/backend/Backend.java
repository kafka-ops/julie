package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import java.io.IOException;

public interface Backend {

  void createOrOpen();

  void createOrOpen(BackendController.Mode mode);

  void saveType(String type);

  void close();

  void save(BackendState state) throws IOException;

  BackendState load() throws IOException;
}
