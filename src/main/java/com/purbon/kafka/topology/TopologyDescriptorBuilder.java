package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TopologyDescriptorBuilder {

  private static final TopologySerdes parser = new TopologySerdes();

  public static Topology build(String fileOrDir) throws IOException {
    List<Topology> topologies = parseListOfTopologies(fileOrDir);
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

  private static List<Topology> parseListOfTopologies(String fileOrDir) throws IOException {
    List<Topology> topologies = new ArrayList<>();
    boolean isDir = Files.isDirectory(Paths.get(fileOrDir));
    if (isDir) {
      Files.list(Paths.get(fileOrDir))
          .sorted()
          .map(
              path -> {
                try {
                  return parser.deserialise(path.toFile());
                } catch (IOException e) {
                  e.printStackTrace();
                  return new TopologyImpl();
                }
              })
          .forEach(subTopology -> topologies.add(subTopology));
    } else {
      Topology firstTopology = parser.deserialise(new File(fileOrDir));
      topologies.add(firstTopology);
    }
    return topologies;
  }
}
