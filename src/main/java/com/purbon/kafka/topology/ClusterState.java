package com.purbon.kafka.topology;

import com.purbon.kafka.topology.clusterstate.FileSateProcessor;
import com.purbon.kafka.topology.clusterstate.StateProcessor;
import com.purbon.kafka.topology.roles.TopologyAclBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClusterState {

    private static final String STATE_TYPE = "acls";

    private final StateProcessor stateProcessor;
    private List<TopologyAclBinding> bindings;

    public ClusterState() {
        this(new FileSateProcessor());
    }

    public ClusterState(StateProcessor stateProcessor) {
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
        stateProcessor.saveType(STATE_TYPE);
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
