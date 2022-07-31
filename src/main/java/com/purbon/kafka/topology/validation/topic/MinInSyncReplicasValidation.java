package com.purbon.kafka.topology.validation.topic;

import com.purbon.kafka.topology.exceptions.ValidationException;
import com.purbon.kafka.topology.model.Topic;
import com.purbon.kafka.topology.validation.TopicValidation;

public class MinInSyncReplicasValidation implements TopicValidation {

  public static final String MIN_INSYNC_REPLICAS = "min.insync.replicas";

  @Override
  public void valid(Topic topic) throws ValidationException {
    if (topic.replicationFactor().isPresent() && !validateMinInsyncReplicas(topic)) {
      String msg =
          String.format(
              "Topic %s has an unexpected min.insync.replicas config vs it's replication factor: %s value",
              topic, topic.replicationFactor().get());
      throw new ValidationException(msg);
    }
  }

  private boolean validateMinInsyncReplicas(Topic topic) {
    short replicationFactor = topic.replicationFactor().orElse((short) -1);
    String minInSyncReplicas = topic.getConfig().get(MIN_INSYNC_REPLICAS);
    return minInSyncReplicas == null
        || (Integer.parseInt(minInSyncReplicas) <= replicationFactor - 1);
  }
}
