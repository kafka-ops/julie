package com.purbon.kafka.topology.api.ccloud.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KafkaAclResponse {

  private String kind;
  private KafkaAclListMetadata metadata;
  private String cluster_id;
  private String resource_type;
  private String resource_name;
  private String pattern_type;
  private String principal;
  private String host;
  private String operation;
  private String permission;
}
