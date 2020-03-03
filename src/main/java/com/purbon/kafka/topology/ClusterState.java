package com.purbon.kafka.topology;

import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.StateProcessor;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.common.acl.AclBinding;

public class ClusterState {

  private final StateProcessor stateProcessor;
  private String type;
  private List<TopologyAclBinding> bindings;


  public ClusterState() {
    this(new FileSateProcessor());
  }

  public ClusterState(StateProcessor stateProcessor) {
    this.type = "acls";
    this.stateProcessor = stateProcessor;
    this.bindings = new ArrayList<>();
  }


  public void update(List<TopologyAclBinding> bindings) {
    this.bindings.addAll(bindings);
  }

  public void forEachBinding(Consumer<? super TopologyAclBinding> action) {
    bindings.forEach(action);
  }

  public void flushAndClose() {
    stateProcessor.saveType(type);
    stateProcessor.saveBindings(bindings);
    stateProcessor.close();
  }

  public void load() throws IOException {
   bindings = stateProcessor.load();
  }

  public void reset() {
    bindings.clear();
    stateProcessor.createOrOpen();
  }

  public int size() {
    return bindings.size();
  }
}
