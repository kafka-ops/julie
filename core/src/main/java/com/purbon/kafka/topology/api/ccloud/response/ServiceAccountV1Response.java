package com.purbon.kafka.topology.api.ccloud.response;

import com.purbon.kafka.topology.model.cluster.ServiceAccountV1;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceAccountV1Response {

  private List<ServiceAccountV1> users;
  private Map<String, Object> page_info;
  private String error;
}
