package com.purbon.kafka.topology.model;

public enum SubjectNameStrategy {
  TOPIC_NAME_STRATEGY("TopicNameStrategy"),
  RECORD_NAME_STRATEGY("RecordNameStrategy"),
  TOPIC_RECORD_NAME_STRATEGY("TopicRecordNameStrategy");

  private final String label;

  SubjectNameStrategy(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }

  public static SubjectNameStrategy valueOfLabel(String label) {
    for (SubjectNameStrategy s : values()) {
      if (s.label.equals(label)) {
        return s;
      }
    }
    return null;
  }
}
