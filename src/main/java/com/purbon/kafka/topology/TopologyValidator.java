package com.purbon.kafka.topology;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Project;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.model.Topology;
import com.purbon.kafka.topology.utils.Either;
import com.purbon.kafka.topology.validation.TopicValidation;
import com.purbon.kafka.topology.validation.TopologyValidation;
import com.purbon.kafka.topology.validation.Validation;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopologyValidator {

  private static final Logger LOGGER = LogManager.getLogger(TopologyValidator.class);

  private final TopologyBuilderConfig config;
  private final String classPrefix;

  public TopologyValidator(TopologyBuilderConfig config) {
    this.config = config;
    this.classPrefix = "com.purbon.kafka.topology.validation.";
  }

  public List<String> validate(Topology topology) {

    Stream<Either<Boolean, ValidationException>> streamOfTopologyResults =
        validations().stream()
            .filter(p -> p instanceof TopologyValidation)
            .map(validation -> (TopologyValidation) validation)
            .map(
                validation -> {
                  try {
                    validation.valid(topology);
                    return Either.Left(true);
                  } catch (ValidationException validationError) {
                    return Either.Right(validationError);
                  }
                });

    List<TopicValidation> listOfTopicValidations =
        validations().stream()
            .filter(p -> p instanceof TopicValidation)
            .map(validation -> (TopicValidation) validation)
            .collect(Collectors.toList());

    Stream<Topic> streamOfTopics =
        topology.getProjects().stream()
            .flatMap((Function<Project, Stream<Topic>>) project -> project.getTopics().stream());

    Stream<Either<Boolean, ValidationException>> streamOfTopicResults =
        streamOfTopics.flatMap(
            (Function<Topic, Stream<Either<Boolean, ValidationException>>>)
                topic ->
                    listOfTopicValidations.stream()
                        .map(
                            validation -> {
                              try {
                                validation.valid(topic);
                                return Either.Left(true);
                              } catch (ValidationException ex) {
                                return Either.Right(ex);
                              }
                            }));

    return Stream.concat(streamOfTopologyResults, streamOfTopicResults)
        .filter(Either::isRight)
        .map(either -> either.getRight().get().getMessage())
        .collect(Collectors.toList());
  }

  class VoidValidation implements TopologyValidation {
    @Override
    public void valid(Topology topology) {}
  }

  private List<Validation> validations() {
    return config.getTopologyValidations().stream()
        .map(
            validationClass -> {
              try {
                String fullValidationClass = classPrefix + validationClass;
                Class<?> clazz = Class.forName(fullValidationClass);
                Constructor<?> constructor = clazz.getConstructor();
                Object instance = constructor.newInstance();
                if (instance instanceof TopologyValidation) {
                  return (TopologyValidation) instance;
                } else if (instance instanceof TopicValidation) {
                  return (TopicValidation) instance;
                } else {
                  throw new IOException("invalid validation type " + fullValidationClass);
                }
              } catch (Exception ex) {
                LOGGER.debug(ex);
                return new VoidValidation();
              }
            })
        .filter(validation -> !(validation instanceof VoidValidation))
        .collect(Collectors.toList());
  }
}
