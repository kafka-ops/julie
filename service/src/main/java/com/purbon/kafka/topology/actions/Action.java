package com.purbon.kafka.topology.actions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public interface Action {

  void run() throws IOException;

  default List<String> refs() {
    return Collections.emptyList();
  }
}
