package server.api.services.Impl;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.Authenticator.Success;
import io.micronaut.core.annotation.Creator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import server.api.model.topology.Topology;
import server.api.models.TopologyCatalog;
import server.api.services.SubscriberHelpers.ObservableSubscriber;
import server.api.services.TopologyService;

import static com.mongodb.client.model.Filters.*;

@Singleton
public class TopologyServiceImpl implements TopologyService {

  private CodecRegistry pojoCodecRegistry;

  @Inject
  private TopologyCatalog catalog;

  @Inject
  private MongoClient mongoClient;

  @Creator
  public TopologyServiceImpl(MongoClient mongoClient) {
    this.catalog = new TopologyCatalog();
    this.mongoClient = mongoClient;

    CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
        .register("server.api.model.topology")
        .automatic(true)
        .build();

    pojoCodecRegistry =
        fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(pojoCodecProvider));
  }

  @Override
  public Topology create(String team) {
    if (catalog.exist(team)) {
      return catalog.getByTeam(team);
    }

    Topology topology = new Topology();
    topology.setTeam(team);
    catalog.addTopology(topology);

    return store(topology);
  }

  @Override
  public Topology update(Topology topology) {

    getCollection()
        .replaceOne(eq("team", topology.getTeam()), topology);

    return topology;
  }

  @Override
  public Topology findByTeam(String team) {
    FindIterable resultsIterator = getCollection()
       .find(eq("team", team));
    List<Topology> results = new ArrayList<>();
    resultsIterator.into(results);
    return results.get(0);
  }

  private Topology store(Topology topology) {

    getCollection()
        .insertOne(topology);
    return topology;
  }

  @Override
  public List<Topology> all() {
    List<Topology> payload = new ArrayList<>();
    getCollection()
        .find().into(payload);
    return payload;
  }

  private MongoCollection getCollection() {
    return getDatabase()
        .getCollection("topologies", Topology.class);
  }

  private MongoDatabase getDatabase() {
    return  mongoClient
        .getDatabase("kafka")
        .withCodecRegistry(pojoCodecRegistry);
  }
}
