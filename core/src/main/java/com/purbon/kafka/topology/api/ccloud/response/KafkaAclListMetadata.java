package com.purbon.kafka.topology.api.ccloud.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KafkaAclListMetadata {

  private String self;
  private String next;
}
