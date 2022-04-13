package com.purbon.kafka.topology.audit;

import com.purbon.kafka.topology.actions.Action;

public class Auditor {

  private Appender appender;

  public Auditor(Appender appender) {
    this.appender = appender;
  }

  public void log(Action action) {
    action.refs().forEach(ref -> appender.log(ref));
  }
}
