package com.purbon.kafka.topology.model.users;

import com.purbon.kafka.topology.model.User;
import java.util.Optional;

public class Producer extends User {

  Optional<String> transactionId;
  Optional<Boolean> idempotence;

  public Producer() {
    super();
    transactionId = Optional.empty();
    idempotence = Optional.empty();
  }

  public Producer(String principal) {
    super(principal);
    transactionId = Optional.empty();
    idempotence = Optional.empty();
  }

  public Optional<String> getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Optional<String> transactionId) {
    this.transactionId = transactionId;
  }

  public Optional<Boolean> getIdempotence() {
    return idempotence;
  }

  public void setIdempotence(Optional<Boolean> idempotence) {
    this.idempotence = idempotence;
  }
}
