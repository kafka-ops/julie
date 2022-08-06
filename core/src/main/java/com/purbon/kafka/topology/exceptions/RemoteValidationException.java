package com.purbon.kafka.topology.exceptions;

import java.io.IOException;

/**
 * Exception raised when a remote validation error has happened. For example, when there are
 * discrepancies between local state and the remote state (someone delete a topic outside of
 * JulieOps)
 */
public class RemoteValidationException extends IOException {
  public RemoteValidationException(String msg) {
    super(msg);
  }
}
