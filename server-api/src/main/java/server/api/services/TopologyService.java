package server.api.services;

import com.purbon.kafka.topology.model.Topology;
import java.util.List;

public interface TopologyService {

  Topology create(String team);

  Topology update(Topology topology);

  Topology findByTeam(String team);

  List<Topology> all();

}
