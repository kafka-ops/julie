package server.api;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class MongoContainer extends GenericContainer {

  public MongoContainer() {
    super("bitnami/mongodb:latest");
    addFixedExposedPort(27017, 27017);
    waitingFor(Wait.forListeningPort());
  }
}
