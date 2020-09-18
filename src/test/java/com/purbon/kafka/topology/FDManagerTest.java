package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.FlatDescription;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.FlatDescriptionSerde;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.kafka.common.Node;
import org.junit.Test;

public class FDManagerTest {
  @Test
  public void test1() throws IOException {
    final TopologySerdes topologySerde = new TopologySerdes();
    final Topology topology = topologySerde.deserialise(new File("example/descriptor.yaml"));

    final FDManager manager = new FDManager();

    FlatDescription fd = manager.compileTopology(topology);
    System.out.println(FlatDescriptionSerde.convertToJsonString(fd, true));
  }

  @Test
  public void test2() throws IOException {
    final String desc1 =
        "---\n"
            + "context: \"context\"\n"
            + "company: \"company\"\n"
            + "env: \"env\"\n"
            + "source: \"source\"\n"
            + "projects:\n"
            + "  - name: \"projectA\"\n"
            + "    consumers:\n"
            + "      - principal: \"User:App0\"\n"
            + "      - principal: \"User:App1\"\n"
            + "    producers:\n"
            + "      - principal: \"User:App3\"\n"
            + "      - principal: \"User:App4\"\n"
            + "    streams:\n"
            + "      - principal: \"User:Streams0\"\n"
            + "        topics:\n"
            + "          read:\n"
            + "            - \"topicA\"\n"
            + "            - \"topicB\"\n"
            + "          write:\n"
            + "            - \"topicC\"\n"
            + "            - \"topicD\"\n"
            + "    connectors:\n"
            + "      - principal: \"User:Connect1\"\n"
            + "        group: \"group\"\n"
            + "        status_topic: \"status\"\n"
            + "        offset_topic: \"offset\"\n"
            + "        configs_topic: \"configs\"\n"
            + "        topics:\n"
            + "          read:\n"
            + "            - \"topicA\"\n"
            + "            - \"topicB\"\n"
            + "      - principal: \"User:Connect2\"\n"
            + "        topics:\n"
            + "          write:\n"
            + "            - \"topicC\"\n"
            + "            - \"topicD\"\n"
            + "    topics:\n"
            + "      - name: \"foo\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n"
            + "      - name: \"bar\"\n"
            + "        dataType: \"avro\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n"
            + "  - name: \"projectB\"\n"
            + "    topics:\n"
            + "      - dataType: \"avro\"\n"
            + "        name: \"bar\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n";

    final String desc2 =
        "---\n"
            + "context: \"context\"\n"
            + "company: \"company\"\n"
            + "env: \"env\"\n"
            + "source: \"source\"\n"
            + "projects:\n"
            + "  - name: \"projectA\"\n"
            + "    consumers:\n"
            + "      - principal: \"User:App0\"\n"
            + "      - principal: \"User:App1\"\n"
            + "    producers:\n"
            + "      - principal: \"User:App3\"\n"
            + "      - principal: \"User:App4\"\n"
            + "    streams:\n"
            + "      - principal: \"User:Streams0\"\n"
            + "        topics:\n"
            + "          read:\n"
            + "            - \"topicA\"\n"
            + "            - \"topicB\"\n"
            + "          write:\n"
            + "            - \"topicC\"\n"
            + "            - \"topicD\"\n"
            + "    connectors:\n"
            + "      - principal: \"User:Connect1\"\n"
            + "        group: \"group\"\n"
            + "        status_topic: \"status\"\n"
            + "        offset_topic: \"offset\"\n"
            + "        configs_topic: \"configs\"\n"
            + "        topics:\n"
            + "          read:\n"
            + "            - \"topicA\"\n"
            + "            - \"topicB\"\n"
            + "      - principal: \"User:Connect2\"\n"
            + "        topics:\n"
            + "          write:\n"
            + "            - \"topicC\"\n"
            + "            - \"topicD\"\n"
            + "    topics:\n"
            + "      - name: \"foo\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n"
            + "      - name: \"bar\"\n"
            + "        dataType: \"avro\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"2\"\n"
            + "  - name: \"projectB\"\n"
            + "    topics:\n"
            + "      - dataType: \"avro\"\n"
            + "        name: \"bar\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n"
            + "      - dataType: \"avro\"\n"
            + "        name: \"bar_new\"\n"
            + "        config:\n"
            + "          replication.factor: \"1\"\n"
            + "          num.partitions: \"1\"\n";

    final TopologySerdes topologySerde = new TopologySerdes();
    final Topology topology1 = topologySerde.deserialise(desc1);
    final Topology topology2 = topologySerde.deserialise(desc2);

    final FDManager manager = new FDManager();

    FlatDescription fd1 = manager.compileTopology(topology1);
    FlatDescription fd2 = manager.compileTopology(topology2);

    final List<FDManager.AbstractAction> abstractActions =
        manager.generatePlan(fd1, fd2, true, new FDManager.DoNothingPolicy());
    System.out.println(abstractActions);

    final List<FDManager.AbstractAction> abstractActions2 =
        manager.generatePlan(fd2, fd1, true, new FDManager.DoNothingPolicy());
    System.out.println(abstractActions2);

    Node node = new Node(0, "localhost", 9092);

    // sadly, MockAdminClient has limited functionality at the moment
    // MockAdminClient mockAdminClient = new MockAdminClient(Collections.singletonList(node), node);
    // System.out.println(mockAdminClient.toString());
  }
}
