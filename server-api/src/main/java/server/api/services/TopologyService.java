package server.api.services;

import java.util.List;
import server.api.model.topology.Topology;

public interface TopologyService {

  Topology create(String team);

  Topology update(Topology topology);

  Topology findByTeam(String team);

  List<Topology> all();

}
