package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.Optional;

public class Producer extends User {

  Optional<String> transactionId;

  public Producer() {
    super();
    transactionId = Optional.empty();
  }

  public Producer(String principal) {
    super(principal);
    transactionId = Optional.empty();
  }

  public Optional<String> getTransactionId() {
    return transactionId;
  }

  public String transactionIdString() {
    return transactionId.orElse("default");
  }

  public void setTransactionId(Optional<String> transactionId) {
    this.transactionId = transactionId;
  }
}
