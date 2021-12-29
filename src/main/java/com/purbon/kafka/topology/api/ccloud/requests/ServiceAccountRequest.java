package com.purbon.kafka.topology.api.ccloud.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class ServiceAccountRequest {

  @NonNull private String display_name;
  @NonNull private String description;
}
