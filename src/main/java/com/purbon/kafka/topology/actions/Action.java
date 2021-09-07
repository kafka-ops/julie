package com.purbon.kafka.topology.actions;

import java.io.IOException;

public interface Action {

  void run() throws IOException;
}
