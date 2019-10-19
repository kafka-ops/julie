import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.TopologySerdes;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.User;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TopologySerdesTest {


  @Test
  public void testTopologySerialisation() throws IOException {

    Topology topology = new Topology();
    topology.setTeam("team");
    topology.setSource("source");
    topology.setProjects(buildProjects());

    TopologySerdes parser = new TopologySerdes();
    String topologyYamlString = parser.serialise(topology);

    Topology deserTopology = parser.deserialise(topologyYamlString);

    Assert.assertEquals(topology.getTeam(), deserTopology.getTeam());
    Assert.assertEquals(topology.getProjects().size(), deserTopology.getProjects().size());
  }



  private List<Project> buildProjects() {

    Project project = new Project();
    project.setName("project");
    project.setConsumers(buildConsumers());
    project.setProducers(buildProducers());
    project.setStreams(buildStreams());

    return Collections.singletonList(project);
  }

  private List<KStream> buildStreams() {
    List<KStream> streams = new ArrayList<KStream>();
    HashMap<String, List<String>> topics = new HashMap<String, List<String>>();
    topics.put("read", Arrays.asList("topic1", "topic3"));
    topics.put("write", Arrays.asList("topic2", "topic4"));
    streams.add(new KStream("app3", topics));

    topics = new HashMap<String, List<String>>();
    topics.put("read", Arrays.asList("topic2", "topic4"));
    topics.put("write", Arrays.asList("topic5"));
    streams.add(new KStream("app4", topics));

    return streams;
  }

  private List<Producer> buildProducers() {
    List<Producer> producers = new ArrayList<Producer>();
    producers.add(new Producer("app1"));
    producers.add(new Producer("app3"));
    return producers;
  }

  private List<Consumer> buildConsumers() {
    List<Consumer> consumers = new ArrayList<Consumer>();
    consumers.add(new Consumer("app1"));
    consumers.add(new Consumer("app2"));
    return consumers;
  }
}
