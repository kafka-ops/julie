package kafka.ops.topology;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import kafka.ops.topology.model.PlanMap;
import kafka.ops.topology.model.Topology;
import kafka.ops.topology.serdes.PlanMapSerdes;
import kafka.ops.topology.serdes.TopologySerdes;

public class TopologyObjectBuilder {

  public static Topology build(String fileOrDir) throws IOException {
    return build(fileOrDir, "", new TopologyBuilderConfig());
  }

  public static Topology build(String fileOrDir, String plansFile) throws IOException {
    return build(fileOrDir, plansFile, new TopologyBuilderConfig());
  }

  public static Topology build(String fileOrDir, TopologyBuilderConfig config) throws IOException {
    return build(fileOrDir, "", config);
  }

  public static Topology build(String fileOrDir, String plansFile, TopologyBuilderConfig config)
      throws IOException {
    PlanMap plans = buildPlans(plansFile);
    List<Topology> topologies = parseListOfTopologies(fileOrDir, config, plans);
    Topology topology = topologies.get(0);
    if (topologies.size() > 1) {
      List<Topology> subTopologies = topologies.subList(1, topologies.size());
      for (Topology subTopology : subTopologies) {
        if (!topology.getContext().equalsIgnoreCase(subTopology.getContext())) {
          throw new IOException("Topologies from different contexts are not allowed");
        }
        subTopology.getProjects().forEach(project -> topology.addProject(project));
      }
    }
    return topology;
  }

  private static PlanMap buildPlans(String plansFile) throws IOException {
    PlanMapSerdes plansSerdes = new PlanMapSerdes();
    return plansFile.isEmpty() ? new PlanMap() : plansSerdes.deserialise(new File(plansFile));
  }

  private static List<Topology> parseListOfTopologies(
      String fileOrDir, TopologyBuilderConfig config, PlanMap plans) throws IOException {
    TopologySerdes parser = new TopologySerdes(config, plans);
    List<Topology> topologies = new ArrayList<>();
    boolean isDir = Files.isDirectory(Paths.get(fileOrDir));
    if (isDir) {
      Files.list(Paths.get(fileOrDir))
          .sorted()
          .map(path -> parser.deserialise(path.toFile()))
          .forEach(subTopology -> topologies.add(subTopology));
    } else {
      Topology firstTopology = parser.deserialise(new File(fileOrDir));
      topologies.add(firstTopology);
    }
    return topologies;
  }
}
