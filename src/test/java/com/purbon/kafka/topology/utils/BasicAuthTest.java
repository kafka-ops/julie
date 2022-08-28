package com.purbon.kafka.topology.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;

public class BasicAuthTest {

  @Test
  void toHttpAuthToken() {
    BasicAuth auth = new BasicAuth("user", "pass");
    assertThat(auth.toHttpAuthToken()).isEqualTo("Basic dXNlcjpwYXNz");
  }

  @Test
  void valuesRequired() {
    assertThatThrownBy(() -> new BasicAuth(null, "pass")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new BasicAuth("user", null)).isInstanceOf(NullPointerException.class);
  }
}
