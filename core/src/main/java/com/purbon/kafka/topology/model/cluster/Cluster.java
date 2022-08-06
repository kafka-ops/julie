package com.purbon.kafka.topology.model.cluster;

public class Cluster {

  private String availability;
  private String id;
  private String name;
  private String provider;
  private String region;
  private String status;
  private String type;

  public Cluster() {
    this("", "", "", "", "", "", "");
  }

  public Cluster(
      String availability,
      String id,
      String name,
      String provider,
      String region,
      String status,
      String type) {
    this.availability = availability;
    this.id = id;
    this.name = name;
    this.provider = provider;
    this.region = region;
    this.status = status;
    this.type = type;
  }

  public String getAvailability() {
    return availability;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getProvider() {
    return provider;
  }

  public String getRegion() {
    return region;
  }

  public String getStatus() {
    return status;
  }

  public String getType() {
    return type;
  }
}
