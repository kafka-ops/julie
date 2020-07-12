package server.api.services;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import server.api.model.topology.Topology;
import com.sun.net.httpserver.Authenticator.Success;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import server.api.models.TopologyCatalog;
import server.api.services.SubscriberHelpers.PrintDocumentSubscriber;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import server.api.services.SubscriberHelpers.PrintSubscriber;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Singleton
public class TopologyService {

  private final CodecRegistry pojoCodecRegistry;

  private TopologyCatalog catalog;

  @Inject
  private MongoClient mongoClient;

  public TopologyService(MongoClient mongoClient) {
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

  public Topology create(String team) {
    if (catalog.exist(team)) {
      return catalog.getByTeam(team);
    }

    Topology topology = new Topology();
    topology.setTeam(team);
    catalog.addTopology(topology);

    return store(topology);
  }

  private Topology store(Topology topology) {
    SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();

    getCollection()
        .insertOne(topology)
        .subscribe(subscriber);
    try {
      subscriber.await();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
    }
    return topology;
  }

  public List<Topology> all() throws Throwable {
    SubscriberHelpers.ObservableSubscriber subscriber = new SubscriberHelpers.ObservableSubscriber<Success>();
    getCollection()
        .find()
        .subscribe(subscriber);
    subscriber.await();

    return subscriber.getReceived();
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
