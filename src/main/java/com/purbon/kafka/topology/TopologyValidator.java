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

  private final Configuration config;

  public TopologyValidator(Configuration config) {
    this.config = config;
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

  private List<Validation> validations() {
    return config.getTopologyValidations().stream()
        .map(
            validationClass -> {
              try {
                Class<?> clazz = getValidationClazz(validationClass);
                if (clazz == null) {
                  throw new IOException(
                      String.format(
                          "Could not find validation class '%s' in class path. "
                              + "Please use the fully qualified class name and check your config.",
                          validationClass));
                }

                Constructor<?> constructor = clazz.getConstructor();
                Object instance = constructor.newInstance();
                if (instance instanceof TopologyValidation) {
                  return (TopologyValidation) instance;
                } else if (instance instanceof TopicValidation) {
                  return (TopicValidation) instance;
                } else {
                  throw new IOException("invalid validation type specified " + validationClass);
                }
              } catch (Exception ex) {
                throw new IllegalStateException(
                    "Failed to load topology validations from class path", ex);
              }
            })
        .collect(Collectors.toList());
  }

  private Class<?> getValidationClazz(String validationClass) {
    try {
      return Class.forName(validationClass);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
