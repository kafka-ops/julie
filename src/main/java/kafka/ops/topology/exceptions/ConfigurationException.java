package kafka.ops.topology.exceptions;

import java.io.IOException;

public class ConfigurationException extends IOException {

  public ConfigurationException(String msg) {
    super(msg);
  }
}
