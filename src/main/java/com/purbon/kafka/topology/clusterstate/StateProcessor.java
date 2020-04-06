package com.purbon.kafka.topology.clusterstate;

import java.io.Reader;
import java.io.Writer;

public interface StateProcessor {

  ClusterState readState(Reader reader);

  void writeState(Writer writer, ClusterState clusterState);
}
