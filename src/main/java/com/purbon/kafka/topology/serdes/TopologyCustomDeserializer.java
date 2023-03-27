package com.purbon.kafka.topology.serdes;

import static com.purbon.kafka.topology.serdes.JsonSerdesUtils.validateRequiresKeys;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Maps;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import com.purbon.kafka.topology.model.*;
import com.purbon.kafka.topology.model.Impl.ProjectImpl;
import com.purbon.kafka.topology.model.Impl.TopologyImpl;
import com.purbon.kafka.topology.model.artefact.*;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.KSqlApp;
import com.purbon.kafka.topology.model.users.KStream;
import com.purbon.kafka.topology.model.users.Other;
import com.purbon.kafka.topology.model.users.Producer;
import com.purbon.kafka.topology.model.users.Schemas;
import com.purbon.kafka.topology.model.users.platform.ControlCenter;
import com.purbon.kafka.topology.model.users.platform.Kafka;
import com.purbon.kafka.topology.model.users.platform.KafkaConnect;
import com.purbon.kafka.topology.model.users.platform.KsqlServer;
import com.purbon.kafka.topology.model.users.platform.SchemaRegistry;
import com.purbon.kafka.topology.utils.Pair;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyCustomDeserializer extends StdDeserializer<Topology> {

  private static final Logger LOGGER = LogManager.getLogger(TopologyCustomDeserializer.class);

  private static final String PROJECTS_KEY = "projects";
  private static final String CONTEXT_KEY = "context";

  private static final String PLATFORM_KEY = "platform";
  private static final String KAFKA_KEY = "kafka";
  private static final String KAFKA_CONNECT_KEY = "kafka_connect";
  private static final String SCHEMA_REGISTRY_KEY = "schema_registry";
  private static final String CONTROL_CENTER_KEY = "control_center";
  private static final String KSQL_KEY = "ksql";

  private static final String NAME_KEY = "name";
  private static final String CONSUMERS_KEY = "consumers";
  private static final String PRODUCERS_KEY = "producers";
  private static final String CONNECTORS_KEY = "connectors";
  private static final String STREAMS_KEY = "streams";
  private static final String SCHEMAS_KEY = "schemas";
  private static final String RBAC_KEY = "rbac";
  private static final String TOPICS_KEY = "topics";
  private static final String PRINCIPAL_KEY = "principal";

  private static final String ACCESS_CONTROL = "access_control";
  private static final String ARTEFACTS = "artefacts";
  private static final String ARTIFACTS = "artifacts";
  private static final String STREAMS_NODE = "streams";
  private static final String TABLES_NODE = "tables";
  private static final String VARS_NODE = "vars";

  private static final String SPECIAL_TOPICS_NODE = "special_topics";

  private static List<String> projectCoreKeys =
      Arrays.asList(
          NAME_KEY,
          CONSUMERS_KEY,
          PRODUCERS_KEY,
          CONNECTORS_KEY,
          STREAMS_KEY,
          SCHEMAS_KEY,
          KSQL_KEY,
          RBAC_KEY);

  private final Configuration config;

  TopologyCustomDeserializer(Configuration config) {
    this(null, config);
  }

  private TopologyCustomDeserializer(Class<?> clazz, Configuration config) {
    super(clazz);
    this.config = config;
  }

  @Override
  public Topology deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {

    JsonNode rootNode = parser.getCodec().readTree(parser);
    validateRequiresKeys(rootNode, CONTEXT_KEY);
    if (rootNode.get(PROJECTS_KEY) == null) {
      LOGGER.warn(
          PROJECTS_KEY
              + " is missing for topology: "
              + rootNode.get(CONTEXT_KEY).asText()
              + ", this might be a required field, be aware.");
    }

    Topology topology = new TopologyImpl(config);
    List<String> excludeAttributes =
        Arrays.asList(PROJECTS_KEY, CONTEXT_KEY, PLATFORM_KEY, SPECIAL_TOPICS_NODE);

    Iterator<String> fieldNames = rootNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      if (!excludeAttributes.contains(fieldName)) {
        topology.addOther(fieldName, rootNode.get(fieldName).asText());
      }
    }
    topology.setContext(rootNode.get(CONTEXT_KEY).asText());

    JsonNode platformNode = rootNode.get(PLATFORM_KEY);
    Platform platform = new Platform();
    if (platformNode != null && platformNode.size() > 0) {
      parse(platformNode, KAFKA_KEY, parser, Kafka.class)
          .ifPresent(obj -> platform.setKafka((Kafka) obj));
      parse(platformNode, KAFKA_CONNECT_KEY, parser, KafkaConnect.class)
          .ifPresent(obj -> platform.setKafkaConnect((KafkaConnect) obj));
      parse(platformNode, SCHEMA_REGISTRY_KEY, parser, SchemaRegistry.class)
          .ifPresent(obj -> platform.setSchemaRegistry((SchemaRegistry) obj));
      parse(platformNode, CONTROL_CENTER_KEY, parser, ControlCenter.class)
          .ifPresent(obj -> platform.setControlCenter((ControlCenter) obj));
      parse(platformNode, KSQL_KEY, parser, KsqlServer.class)
          .ifPresent(obj -> platform.setKsqlServer((KsqlServer) obj));
    } else {
      LOGGER.debug("No platform components defined in the topology.");
    }

    topology.setPlatform(platform);
    if (rootNode.get(PROJECTS_KEY) != null) {
      parseProjects(parser, rootNode.get(PROJECTS_KEY), topology, config)
          .forEach(topology::addProject);

      // validate the generated full topics names for valid encoding
      for (Project project : topology.getProjects()) {
        for (Topic topic : project.getTopics()) {
          validateEncodingForTopicName(topic.toString());
        }
      }
    }

    JsonNode specialTopicsNode = rootNode.get(SPECIAL_TOPICS_NODE);
    if (specialTopicsNode != null && specialTopicsNode.size() > 0) {
      for (int i = 0; i < specialTopicsNode.size(); i++) {
        JsonNode node = specialTopicsNode.get(i);
        var topic = parser.getCodec().treeToValue(node, Topic.class);
        topology.addSpecialTopic(topic);
      }
    }

    return topology;
  }

  private Optional<Object> parse(JsonNode node, String key, JsonParser parser, Class klass)
      throws JsonProcessingException {
    JsonNode pNode = node.get(key);
    if (pNode == null) {
      LOGGER.debug(String.format("%s key is missing.", key));
      return Optional.empty();
    }
    Object obj = parser.getCodec().treeToValue(pNode, klass);
    LOGGER.debug(String.format("Extracting key %s with value %s", key, obj));
    return Optional.of(obj);
  }

  private List<Project> parseProjects(
      JsonParser parser, JsonNode projectsNode, Topology topology, Configuration config)
      throws IOException {
    List<Project> projects = new ArrayList<>();
    for (int i = 0; i < projectsNode.size(); i++) {
      Project project = parseProject(parser, projectsNode.get(i), topology, config);
      LOGGER.debug(
          String.format(
              "Adding project %s to the Topology %s", project.getName(), topology.getContext()));
      projects.add(project);
    }
    return projects;
  }

  private Project parseProject(
      JsonParser parser, JsonNode rootNode, Topology topology, Configuration config)
      throws IOException {

    Iterable<String> it = () -> rootNode.fieldNames();
    List<String> keys =
        StreamSupport.stream(it.spliterator(), false)
            .filter(key -> !Arrays.asList(TOPICS_KEY, NAME_KEY).contains(key))
            .collect(Collectors.toList());
    Map<String, JsonNode> rootNodes = Maps.asMap(new HashSet<>(keys), (key) -> rootNode.get(key));

    Map<String, PlatformSystem> mapOfValues = new HashMap<>();
    for (String key : rootNodes.keySet()) {
      JsonNode keyNode = rootNodes.get(key);
      if (keyNode != null) {
        Optional<PlatformSystem> optionalPlatformSystem;
        switch (key) {
          case CONSUMERS_KEY:
            optionalPlatformSystem = doConsumerElements(parser, keyNode);
            break;
          case PRODUCERS_KEY:
            optionalPlatformSystem = doProducerElements(parser, keyNode);
            break;
          case CONNECTORS_KEY:
            optionalPlatformSystem = doKafkaConnectElements(parser, keyNode);
            break;
          case STREAMS_KEY:
            optionalPlatformSystem = doStreamsElements(parser, keyNode);
            break;
          case SCHEMAS_KEY:
            optionalPlatformSystem = doSchemasElements(parser, keyNode);
            break;
          case KSQL_KEY:
            optionalPlatformSystem = doKSqlElements(parser, keyNode);
            break;
          default:
            optionalPlatformSystem = Optional.empty();
            if (!key.equalsIgnoreCase(RBAC_KEY)) {
              optionalPlatformSystem = doOtherElements(parser, keyNode);
            }
        }
        optionalPlatformSystem.ifPresent(ps -> mapOfValues.put(key, ps));
      }
    }

    ProjectImpl project =
        new ProjectImpl(
            rootNode.get(NAME_KEY).asText(),
            Optional.ofNullable(mapOfValues.get(CONSUMERS_KEY)),
            Optional.ofNullable(mapOfValues.get(PRODUCERS_KEY)),
            Optional.ofNullable(mapOfValues.get(STREAMS_KEY)),
            Optional.ofNullable(mapOfValues.get(CONNECTORS_KEY)),
            Optional.ofNullable(mapOfValues.get(SCHEMAS_KEY)),
            Optional.ofNullable(mapOfValues.get(KSQL_KEY)),
            parseOptionalRbacRoles(rootNode.get(RBAC_KEY)),
            filterOthers(mapOfValues),
            config);

    project.setPrefixContextAndOrder(topology.asFullContext(), topology.getOrder());

    var topicsNode = rootNode.get(TOPICS_KEY);
    if (topicsNode == null) {
      LOGGER.warn(
          TOPICS_KEY
              + " is missing for project: "
              + project.getName()
              + ", this might be a required field, be aware.");
    } else {
      var allowList =
          config.getDlqTopicsAllowList().stream()
              .map(Pattern::compile)
              .collect(Collectors.toList());
      var denyList =
          config.getDlqTopicsDenyList().stream().map(Pattern::compile).collect(Collectors.toList());
      new JsonSerdesUtils<Topic>()
          .parseApplicationUser(parser, topicsNode, Topic.class)
          .forEach(
              topic -> {
                project.addTopic(topic); // add normal topic and evaluate
                if (config.shouldGenerateDlqTopics()) {
                  String name = topic.toString();
                  if (shouldGenerateDlqTopic(allowList, denyList).apply(name)) {
                    Topic dlqTopic = topic.clone();
                    dlqTopic.setDlqPrefix(config.getDlqTopicLabel());
                    dlqTopic.setTopicNamePattern(config.getDlqTopicPrefixFormat());
                    project.addTopic(dlqTopic);
                  }
                }
              });
    }

    return project;
  }

  private Function<String, Boolean> shouldGenerateDlqTopic(
      List<Pattern> allowList, List<Pattern> denyList) {
    return name -> {
      var foundInAllowList =
          allowList.stream().map(e -> e.matcher(name).matches()).anyMatch(p -> p);
      var foundInDenyList = denyList.stream().map(e -> e.matcher(name).matches()).anyMatch(p -> p);

      var isAllowedOrEmpty = allowList.isEmpty() || foundInAllowList;
      var isNotDeniedOrEmpty = denyList.isEmpty() || !foundInDenyList;
      return isAllowedOrEmpty && isNotDeniedOrEmpty;
    };
  }

  private List<Map.Entry<String, PlatformSystem<Other>>> filterOthers(
      Map<String, PlatformSystem> mapOfValues) {
    return mapOfValues.entrySet().stream()
        .filter(entry -> !projectCoreKeys.contains(entry.getKey()))
        .map(entry -> Map.entry(entry.getKey(), (PlatformSystem<Other>) entry.getValue()))
        .collect(Collectors.toList());
  }

  private Optional<PlatformSystem> doOtherElements(JsonParser parser, JsonNode node)
      throws JsonProcessingException {
    List<Other> others =
        new JsonSerdesUtils<Other>().parseApplicationUser(parser, node, Other.class);
    return Optional.of(new PlatformSystem(others));
  }

  private Optional<PlatformSystem> doConsumerElements(JsonParser parser, JsonNode node)
      throws JsonProcessingException {
    List<Consumer> consumers =
        new JsonSerdesUtils<Consumer>().parseApplicationUser(parser, node, Consumer.class);
    return Optional.of(new PlatformSystem(consumers));
  }

  private Optional<PlatformSystem> doProducerElements(JsonParser parser, JsonNode node)
      throws JsonProcessingException {
    List<Producer> producers =
        new JsonSerdesUtils<Producer>().parseApplicationUser(parser, node, Producer.class);
    return Optional.of(new PlatformSystem(producers));
  }

  private Optional<PlatformSystem> doKafkaConnectElements(JsonParser parser, JsonNode node)
      throws IOException {

    JsonNode acNode = node;
    if (node.has(ACCESS_CONTROL)) {
      acNode = node.get(ACCESS_CONTROL);
    }
    List<Connector> connectors =
        new JsonSerdesUtils<Connector>().parseApplicationUser(parser, acNode, Connector.class);
    List<KafkaConnectArtefact> artefacts = Collections.emptyList();
    if (node.has(ARTEFACTS) || node.has(ARTIFACTS)) {
      String key = node.has(ARTEFACTS) ? ARTEFACTS : ARTIFACTS;
      artefacts =
          new JsonSerdesUtils<KafkaConnectArtefact>()
              .parseApplicationUser(parser, node.get(key), KafkaConnectArtefact.class);
      Set<String> serverLabels = config.getKafkaConnectServers().keySet();
      for (KafkaConnectArtefact artefact : artefacts) {
        if (artefact.getPath() == null
            || artefact.getServerLabel() == null
            || artefact.getName() == null) {
          throw new TopologyParsingException(
              "KafkaConnect: Path, name and label are artefact mandatory fields");
        }
        if (!serverLabels.contains(artefact.getServerLabel())) {
          throw new TopologyParsingException(
              String.format(
                  "KafkaConnect: Server alias label %s does not exist on the provided configuration, please check",
                  artefact.getServerLabel()));
        }
      }
    }
    // bloody hack that needs to be cleanned. This is to support not having ACLS defined properly
    // and only connectors.
    if (connectors.size() == 1 && connectors.get(0) == null) {
      connectors = new ArrayList<>();
    }
    return Optional.of(new PlatformSystem(connectors, new KConnectArtefacts(artefacts)));
  }

  private Optional<PlatformSystem> doKSqlElements(JsonParser parser, JsonNode node)
      throws JsonProcessingException {

    JsonNode acNode = node;
    if (node.has(ACCESS_CONTROL)) {
      acNode = node.get(ACCESS_CONTROL);
    }

    List<KSqlApp> ksqls =
        new JsonSerdesUtils<KSqlApp>().parseApplicationUser(parser, acNode, KSqlApp.class);
    List<KsqlStreamArtefact> streamArtefacts = new ArrayList<>();
    List<KsqlTableArtefact> tableArtefacts = new ArrayList<>();
    KsqlVarsArtefact varsArtefacts = new KsqlVarsArtefact(Collections.emptyMap());

    if (node.has(ARTEFACTS) || node.has(ARTIFACTS)) {
      String key = node.has(ARTEFACTS) ? ARTEFACTS : ARTIFACTS;
      JsonNode artefactsNode = node.get(key);
      if (artefactsNode.has(STREAMS_NODE)) {
        streamArtefacts =
            new JsonSerdesUtils<KsqlStreamArtefact>()
                .parseApplicationUser(
                    parser, artefactsNode.get(STREAMS_NODE), KsqlStreamArtefact.class);
      }

      if (artefactsNode.has(TABLES_NODE)) {
        tableArtefacts =
            new JsonSerdesUtils<KsqlTableArtefact>()
                .parseApplicationUser(
                    parser, artefactsNode.get(TABLES_NODE), KsqlTableArtefact.class);
      }

      if (artefactsNode.has(VARS_NODE)) {
        artefactsNode.get(VARS_NODE);
        varsArtefacts.setSessionVars(
            parser.getCodec().treeToValue(artefactsNode.get(VARS_NODE), Map.class));
      }
    }

    return Optional.of(
        new PlatformSystem(
            ksqls, new KsqlArtefacts(streamArtefacts, tableArtefacts, varsArtefacts)));
  }

  private Optional<PlatformSystem> doStreamsElements(JsonParser parser, JsonNode node)
      throws IOException {
    List<KStream> streams =
        new JsonSerdesUtils<KStream>()
            .parseApplicationUser(parser, node, KStream.class).stream()
                .map(
                    ks -> {
                      ks.getTopics().putIfAbsent(KStream.READ_TOPICS, Collections.emptyList());
                      ks.getTopics().putIfAbsent(KStream.WRITE_TOPICS, Collections.emptyList());
                      return ks;
                    })
                .collect(Collectors.toList());

    for (KStream ks : streams) {
      var topics = ks.getTopics();
      if (topics.get(KStream.READ_TOPICS).isEmpty() || topics.get(KStream.WRITE_TOPICS).isEmpty()) {
        LOGGER.warn(
            "A Kafka Streams application with Id ("
                + ks.getApplicationId()
                + ") and Principal ("
                + ks.getPrincipal()
                + ")"
                + " might require both read and write topics as per its "
                + "nature it is always reading and writing into Apache Kafka, be aware if you notice problems.");
      }
      if (topics.get(KStream.READ_TOPICS).isEmpty()) {
        // should have at minimum read topics defined as we could think of write topics as internal
        // topics being
        // auto created.
        throw new IOException(
            "Kafka Streams application with Id "
                + ks.getApplicationId()
                + " and principal "
                + ks.getPrincipal()
                + " have missing read topics. This field is required.");
      }
    }
    return Optional.of(new PlatformSystem(streams));
  }

  private Optional<PlatformSystem> doSchemasElements(JsonParser parser, JsonNode node)
      throws JsonProcessingException {
    List<Schemas> schemas =
        new JsonSerdesUtils<Schemas>().parseApplicationUser(parser, node, Schemas.class);
    return Optional.of(new PlatformSystem(schemas));
  }

  private void validateEncodingForTopicName(String name) throws IOException {
    Pattern p = Pattern.compile("^[\\x00-\\x7F\\._-]+$");
    if (!p.matcher(name).matches()) {
      String validCharacters = "ASCII alphanumerics, '.', '_' and '-'";
      throw new IOException(
          " Topic name \""
              + name
              + "\" is illegal, it contains a character other than "
              + validCharacters);
    }
  }

  private Map<String, List<String>> parseOptionalRbacRoles(JsonNode rbacRootNode) {
    if (rbacRootNode == null) return new HashMap<>();
    return StreamSupport.stream(rbacRootNode.spliterator(), true)
        .map(
            (Function<JsonNode, Pair<String, JsonNode>>)
                node -> {
                  String key = node.fieldNames().next();
                  return new Pair(key, node.get(key));
                })
        .flatMap(
            (Function<Pair<String, JsonNode>, Stream<Pair<String, String>>>)
                principals ->
                    StreamSupport.stream(principals.getValue().spliterator(), true)
                        .map(
                            node ->
                                new Pair<>(principals.getKey(), node.get(PRINCIPAL_KEY).asText())))
        .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, toList())));
  }
}
