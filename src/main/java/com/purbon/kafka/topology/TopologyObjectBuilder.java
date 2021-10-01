package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.PlanMapSerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologyObjectBuilder {

  public static Map<String, Topology> build(String fileOrDir) throws IOException {
    return build(fileOrDir, "", new Configuration());
  }

  public static Map<String, Topology> build(String fileOrDir, String plansFile) throws IOException {
    return build(fileOrDir, plansFile, new Configuration());
  }

  public static Map<String, Topology> build(String fileOrDir, Configuration config)
      throws IOException {
    return build(fileOrDir, "", config);
  }

  public static Map<String, Topology> build(
      String fileOrDir, String plansFile, Configuration config) throws IOException {
    PlanMap plans = buildPlans(plansFile);
    List<Topology> topologies = parseListOfTopologies(fileOrDir, config, plans);
    Map<String, Topology> collection = new HashMap<>();

    for (Topology topology : topologies) {
      String context = topology.getContext();
      if (!collection.containsKey(context)) {
        collection.put(context, topology);
      } else {
        Topology mainTopology = collection.get(context);
        topology.getProjects().forEach(p -> mainTopology.addProject(p));
        collection.put(context, mainTopology);
      }
    }
    return collection;
  }

  private static PlanMap buildPlans(String plansFile) throws IOException {
    PlanMapSerdes plansSerdes = new PlanMapSerdes();
    return plansFile.isEmpty() ? new PlanMap() : plansSerdes.deserialise(new File(plansFile));
  }

  private static List<Topology> parseListOfTopologies(
      String fileOrDir, Configuration config, PlanMap plans) throws IOException {
    TopologySerdes parser = new TopologySerdes(config, plans);
    List<Topology> topologies = new ArrayList<>();
    boolean isDir = Files.isDirectory(Paths.get(fileOrDir));
    if (isDir) {
      Files.list(Paths.get(fileOrDir))
          .sorted()
          .filter(p -> !Files.isDirectory(p))
          .map(path -> parser.deserialise(path.toFile()))
          .forEach(subTopology -> topologies.add(subTopology));
    } else {
      Topology firstTopology = parser.deserialise(new File(fileOrDir));
      topologies.add(firstTopology);
    }
    return topologies;
  }
}
