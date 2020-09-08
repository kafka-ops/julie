package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.addTopics2Project;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import java.io.IOException;
import java.util.*;

public class ProjectCustomDeserializer extends StdDeserializer<ProjectImpl> {

  public static final String NAME_KEY = "name";
  public static final String CONSUMERS_KEY = "consumers";
  public static final String PRODUCERS_KEY = "producers";
  public static final String CONNECTORS_KEY = "connectors";
  public static final String STREAMS_KEY = "streams";
  public static final String SCHEMAS_KEY = "schemas";
  public static final String RBAC_KEY = "rbac";
  public static final String TOPICS_KEY = "topics";
  public static final String PRINCIPAL_KEY = "principal";

  protected ProjectCustomDeserializer() {
    this(null);
  }

  protected ProjectCustomDeserializer(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public ProjectImpl deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);
    ProjectImpl project = new ProjectImpl();

    String nameFieldValue = rootNode.get(NAME_KEY).asText();
    project.setName(nameFieldValue);

    JsonSerdesUtils<Consumer> consumerSerdes = new JsonSerdesUtils<>();
    JsonNode consumers = rootNode.get(CONSUMERS_KEY);
    if (consumers != null) {
      List<Consumer> consumersList =
          consumerSerdes.parseApplicationUser(parser, consumers, Consumer.class);
      project.setConsumers(consumersList);
    }

    JsonSerdesUtils<Producer> producerSerdes = new JsonSerdesUtils<>();
    JsonNode producers = rootNode.get(PRODUCERS_KEY);
    if (producers != null) {
      List<Producer> producersList =
          producerSerdes.parseApplicationUser(parser, producers, Producer.class);
      project.setProducers(producersList);
    }

    JsonSerdesUtils<Connector> connectorSerdes = new JsonSerdesUtils<>();
    JsonNode connectors = rootNode.get(CONNECTORS_KEY);
    if (connectors != null) {
      List<Connector> connectorList =
          connectorSerdes.parseApplicationUser(parser, connectors, Connector.class);
      project.setConnectors(connectorList);
    }

    JsonSerdesUtils<KStream> streamsSerdes = new JsonSerdesUtils<>();
    JsonNode streams = rootNode.get(STREAMS_KEY);
    if (streams != null) {
      List<KStream> streamsList =
          streamsSerdes.parseApplicationUser(parser, streams, KStream.class);
      project.setStreams(streamsList);
    }

    JsonSerdesUtils<Schemas> schemasSerdes = new JsonSerdesUtils<>();
    JsonNode schemas = rootNode.get(SCHEMAS_KEY);
    if (schemas != null) {
      List<Schemas> schemasList =
          schemasSerdes.parseApplicationUser(parser, schemas, Schemas.class);
      project.setSchemas(schemasList);
    }

    // Parser optional RBAC object, only there if using RBAC provider
    JsonNode rbacRootNode = rootNode.get(RBAC_KEY);
    if (rbacRootNode != null) {
      Map<String, List<String>> roles = parseOptionalRbacRoles(rbacRootNode);
      project.setRbacRawRoles(roles);
    }

    JsonNode topics = rootNode.get(TOPICS_KEY);
    addTopics2Project(parser, project, topics);

    return project;
  }

  private Map<String, List<String>> parseOptionalRbacRoles(JsonNode rbacRootNode) {
    Map<String, List<String>> roles = new HashMap<>();
    for (int i = 0; i < rbacRootNode.size(); i++) {
      JsonNode elem = rbacRootNode.get(i);
      Iterator<String> fields = elem.fieldNames();
      while (fields.hasNext()) {
        String field = fields.next(); // field == RoleName
        List<String> principalsByRole = new ArrayList<>();
        JsonNode principals = elem.get(field);
        for (int j = 0; j < principals.size(); j++) {
          JsonNode principalNode = principals.get(j);
          String principal = principalNode.get(PRINCIPAL_KEY).asText();
          principalsByRole.add(principal);
        }
        roles.put(field, principalsByRole);
      }
    }
    return roles;
  }
}
