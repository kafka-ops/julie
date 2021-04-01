package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.storage.*;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GCPBackend extends AbstractBackend {

  private static final Logger LOGGER = LogManager.getLogger(GCPBackend.class);

  private Storage storage;
  private Configuration config;

  @Override
  public void configure(Configuration config) {
    configure(config, null);
  }

  @Override
  public Set<ServiceAccount> loadServiceAccounts() throws IOException {
    try {
      String content = load(SA_FILE_NAME);
      return JSON.toSASet(content);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public Set<TopologyAclBinding> loadBindings() throws IOException {
    try {
      String content = load(BINDINGS_FILE_NAME);
      return JSON.toBindingsSet(content);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public Set<String> loadTopics() throws IOException {
    try {
      String content = load(TOPIC_FILE_NAME);
      return JSON.toStringsSet(content);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public void saveType(String type) {}

  @Override
  public void saveBindings(Set<TopologyAclBinding> bindings) {
    try {
      String content = JSON.asString(bindings);
      save(content, BINDINGS_FILE_NAME);
    } catch (JsonProcessingException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void saveAccounts(Set<ServiceAccount> accounts) {
    try {
      String content = JSON.asString(accounts);
      save(content, SA_FILE_NAME);
    } catch (JsonProcessingException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void saveTopics(Set<String> topics) {
    try {
      String content = JSON.asString(topics);
      save(content, TOPIC_FILE_NAME);
    } catch (JsonProcessingException e) {
      LOGGER.error(e);
    }
  }

  public void configure(Configuration config, URI endpoint) {
    this.config = config;
    this.storage =
        StorageOptions.newBuilder().setProjectId(config.getGCPProjectId()).build().getService();
  }

  private String load(String filename) {
    Blob blob = storage.get(BlobId.of(config.getGCPBucket(), filename));
    return new String(blob.getContent(), StandardCharsets.UTF_8);
  }

  private void save(String content, String filename) {
    BlobId blobId = BlobId.of(config.getGCPBucket(), filename);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    try {
      storage.create(
          blobInfo,
          content.getBytes(StandardCharsets.UTF_8),
          Storage.BlobTargetOption.detectContentType());
    } catch (Exception ex) {
      LOGGER.error(ex);
    }
  }

  @Override
  public void close() {
    // empty
  }
}
