package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.PlanMap;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.serdes.PlanMapSerdes;
import com.purbon.kafka.topology.serdes.TopologySerdes;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
      if (!config.areMultipleContextPerDirEnabled()
          && (!collection.containsKey(context) && collection.size() == 1)) {
        // the parsing found a new topology with a different context, as it is not enabled
        // it should be flag as error
        throw new IOException("Topologies from different contexts are not allowed");
      }
      if (!collection.containsKey(context)) {
        collection.put(context, topology);
      } else {
        Topology mainTopology = collection.get(context);
        List<String> projectNames =
            mainTopology.getProjects().stream()
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.toList());

        for (Project project : topology.getProjects()) {
          if (projectNames.contains(project.getName().toLowerCase())) {
            throw new IOException(
                "Trying to add a project with name "
                    + project.getName()
                    + " in a sub topology (context: "
                    + mainTopology.getContext()
                    + ") that already contain the same project. Merging projects is not yet supported");
          }
          mainTopology.addProject(project);
        }

        for (String other : topology.getOrder()) {
          var topologyContext = topology.asFullContext();
          if (!mainTopology.getOrder().contains(other)) {
            String value = String.valueOf(topologyContext.get(other));
            mainTopology.addOther(other, value);
          }
        }
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
    final Path path = Paths.get(fileOrDir);
    if (Files.isDirectory(path)) {
      loadFromDirectory(path, config.isRecursive(), parser, topologies);
    } else {
      topologies.add(parser.deserialise(new File(fileOrDir)));
    }
    return topologies;
  }

  private static void loadFromDirectory(
      final Path directory,
      final boolean recursive,
      final TopologySerdes parser,
      final List<Topology> topologies) {
    try {
      Files.list(directory)
          .sorted()
          .filter(p -> !Files.isDirectory(p))
          .map(path -> parser.deserialise(path.toFile()))
          .forEach(topologies::add);
      if (recursive) {
        Files.list(directory)
            .sorted()
            .filter(Files::isDirectory)
            .forEach(p -> loadFromDirectory(p, recursive, parser, topologies));
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
