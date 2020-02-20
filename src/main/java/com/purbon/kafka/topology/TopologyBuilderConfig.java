package com.purbon.kafka.topology;

public class TopologyBuilderConfig {

  public static final String ACCESS_CONTROL_IMPLEMENTATION_CLASS = "topology.builder.access.control.class";
  public static final String ACCESS_CONTROL_DEFAULT_CLASS = "com.purbon.kafka.topology.roles.SimpleAclsProvider";
}
