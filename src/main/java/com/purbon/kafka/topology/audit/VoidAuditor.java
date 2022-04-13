package com.purbon.kafka.topology.audit;

import com.purbon.kafka.topology.actions.Action;

public class VoidAuditor extends Auditor {

  public VoidAuditor() {
    super(new StdoutAppender());
  }

  @Override
  public void log(Action action) {
    // NO-OP
  }
}
