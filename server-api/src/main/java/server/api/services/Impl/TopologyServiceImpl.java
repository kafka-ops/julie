package server.api.services.Impl;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopicImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import io.micronaut.core.annotation.Creator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;
import server.api.helpers.OptionalPropertyCodecProvider;
import server.api.models.TopologyCatalog;
import server.api.models.TopologyDeco;
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

    ClassModel<Project> projectModel = ClassModel.builder(Project.class).enableDiscriminator(true).build();
    ClassModel<ProjectImpl> projectImplModel = ClassModel.builder(ProjectImpl.class).enableDiscriminator(true).build();

    ClassModel<Topic> topicModel = ClassModel.builder(Topic.class).enableDiscriminator(true).build();
    ClassModel<TopicImpl> topicImplModel = ClassModel.builder(TopicImpl.class).enableDiscriminator(true).build();


    CodecProvider defaultPojoCodecProvider = PojoCodecProvider.builder()
        .register(
            "com.purbon.kafka.topology.model.Impl",
            "com.purbon.kafka.topology.model",
            "com.purbon.kafka.topology.model.users",
            "server.api.models")
        .register(new OptionalPropertyCodecProvider())
        .automatic(true)
        .build();

    PojoCodecProvider mappedPojoCodecProvider = PojoCodecProvider.builder()
        .register(projectModel, projectImplModel)
        .register(topicModel, topicImplModel)
        .register(new OptionalPropertyCodecProvider())
        .build();

    pojoCodecRegistry =
        fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(mappedPojoCodecProvider, defaultPojoCodecProvider));
  }

  @Override
  public TopologyDeco create(String team) {
    if (catalog.exist(team)) {
      return catalog.getByTeam(team);
    }

    TopologyDeco topology = new TopologyDeco();
    topology.setTeam(team);
    catalog.addTopology(topology);

    return store(topology);
  }

  @Override
  public TopologyDeco update(TopologyDeco topology) {

    getCollection()
        .replaceOne(eq("team", topology.getTeam()), topology);

    return topology;
  }

  @Override
  public TopologyDeco findByTeam(String team) {
    FindIterable resultsIterator = getCollection()
       .find(eq("team", team));
    List<TopologyDeco> results = new ArrayList<>();
    resultsIterator.into(results);
    return results.get(0);
  }

  private TopologyDeco store(TopologyDeco topology) {

    getCollection()
        .insertOne(topology);
    return topology;
  }

  @Override
  public List<TopologyDeco> all() {
    List<TopologyDeco> payload = new ArrayList<>();
    getCollection()
        .find().into(payload);
    return payload;
  }

  private MongoCollection getCollection() {
    return getDatabase()
        .getCollection("topologies", TopologyDeco.class);
  }

  private MongoDatabase getDatabase() {
    return  mongoClient
        .getDatabase("kafka")
        .withCodecRegistry(pojoCodecRegistry);
  }
}
