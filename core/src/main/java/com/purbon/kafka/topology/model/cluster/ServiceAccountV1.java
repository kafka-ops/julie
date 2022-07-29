package com.purbon.kafka.topology.model.cluster;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceAccountV1 {

  private long id;
  private String email;
  private String first_name;
  private String last_name;
  private int organization_id;
  private boolean deactivated;
  private String verified;
  private String created;
  private String modified;
  private String service_name;
  private String service_description;
  private boolean service_account;
  private Map<String, Object> sso;
  private Map<String, Object> preferences;
  private boolean internal;
  private String resource_id;
  private String deactivated_at;
  private String social_connection;
  private String auth_type;

  // visible for test
  public ServiceAccountV1(Long id, String email, String service_name, String resource_id) {
    this.id = id;
    this.email = email;
    this.service_name = service_name;
    this.resource_id = resource_id;
  }
}
