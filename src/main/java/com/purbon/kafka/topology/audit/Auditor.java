package com.purbon.kafka.topology.audit;

import com.purbon.kafka.topology.actions.Action;
import lombok.Getter;

public class Auditor {

  @Getter private Appender appender;

  public Auditor(Appender appender) {
    this.appender = appender;
  }

  public void log(Action action) {
    action.refs().forEach(ref -> appender.log(ref));
  }
}
