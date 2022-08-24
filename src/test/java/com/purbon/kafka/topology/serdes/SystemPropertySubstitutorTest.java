package com.purbon.kafka.topology.serdes;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SystemPropertySubstitutorTest {
  @Test
  public void standardReplacement() {
    // Given
    System.getProperties().setProperty("env", "staging");
    System.getProperties().setProperty("project", "my_project");
    SystemPropertySubstitutor systemPropertySubstitutor = new SystemPropertySubstitutor();

    // When
    String result = systemPropertySubstitutor.replace(
      "context: ${env}\n"
      + "project: ${project}");

    // Then
    assertThat(result).isEqualTo(
      "context: staging\n"
        + "project: my_project"
    );
  }

  @Test
  public void notFoundKeyReplacement() {
    // Given
    SystemPropertySubstitutor systemPropertySubstitutor = new SystemPropertySubstitutor();

    // When
    // Then
    assertThatThrownBy(() -> systemPropertySubstitutor.replace("${not_found_key}"))
      .isExactlyInstanceOf(TopologyParsingException.class);
  }
}