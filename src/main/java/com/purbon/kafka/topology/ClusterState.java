package com.purbon.kafka.topology;

import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClusterState {

  private String type;
  private PrintWriter writer;
  private List<TopologyAclBinding> bindings;

  public ClusterState() {
    this.type = "acls";
    this.bindings = new ArrayList<>();
    try {
      this.writer = new PrintWriter(".cluster-state");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void update(List<TopologyAclBinding> bindings) {
    this.bindings.addAll(bindings);
  }

  public void flushAndClose() {
    writer.println(type);
    bindings.forEach(binding -> writer.println(binding));
    this.writer.close();
  }
}
