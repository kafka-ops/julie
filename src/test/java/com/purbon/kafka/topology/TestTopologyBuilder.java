package com.purbon.kafka.topology;

import com.purbon.kafka.topology.model.impl.ProjectImpl;
import com.purbon.kafka.topology.model.impl.TopicImpl;
import com.purbon.kafka.topology.model.impl.TopologyImpl;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.model.users.Consumer;
import com.purbon.kafka.topology.model.users.Producer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TestTopologyBuilder {
  private final Configuration configuration;
  private final Topology topology;
  private final Project project;
  private Set<Topic> topics = new HashSet<>();
  private final Set<Consumer> consumers = new HashSet<>();
  private final Set<Producer> producers = new HashSet<>();

  public TestTopologyBuilder() {
    this(new Configuration(), "ctx", "project");
  }

  public TestTopologyBuilder(Configuration configuration, String context, String projectName) {
    this.configuration = configuration;
    topology = new TopologyImpl(configuration);
    topology.setContext(context);
    project = new ProjectImpl(projectName, configuration);
    topology.addProject(project);
  }

  public static TestTopologyBuilder createProject() {
    return new TestTopologyBuilder();
  }

  public static TestTopologyBuilder createProject(String context, String projectName) {
    return new TestTopologyBuilder(new Configuration(), context, projectName);
  }

  public static TestTopologyBuilder createProject(Configuration configuration) {
    return new TestTopologyBuilder(configuration, "ctx", "project");
  }

  public Topology buildTopology() {
    project.setTopics(new ArrayList<>(topics));
    project.setConsumers(new ArrayList<>(consumers));
    project.setProducers(new ArrayList<>(producers));
    return topology;
  }

  public TestTopologyBuilder addTopic(String topicName) {
    addTopic(new TopicImpl(topicName, configuration));
    return this;
  }

  public TestTopologyBuilder addTopic(Topic topic) {
    topics.add(topic);
    return this;
  }

  @SuppressWarnings("unused")
  public TestTopologyBuilder removeTopic(String topicName) {
    topics =
        topics.stream()
            .filter(topic -> !topic.getName().equals(topicName))
            .collect(Collectors.toSet());
    return this;
  }

  public TestTopologyBuilder addConsumer(String user) {
    consumers.add(new Consumer(user));
    return this;
  }

  public TestTopologyBuilder addConsumer(String user, String group) {
    Consumer consumer = new Consumer(user);
    consumer.setGroup(Optional.of(group));
    consumers.add(consumer);
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public TestTopologyBuilder removeConsumer(String user) {
    consumers.remove(new Consumer(user));
    return this;
  }

  public TestTopologyBuilder addProducer(String user) {
    producers.add(new Producer(user));
    return this;
  }

  @SuppressWarnings("unused")
  public TestTopologyBuilder removeProducer(String user) {
    producers.remove(new Producer(user));
    return this;
  }

  public Topic getTopic(String topicName) {
    return topics.stream()
        .filter(topic1 -> topic1.getName().equals(topicName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No topic named " + topicName));
  }

  public Set<Consumer> getConsumers() {
    return consumers;
  }

  public Set<Producer> getProducers() {
    return producers;
  }

  public Project getProject() {
    return project;
  }
}
