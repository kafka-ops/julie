package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.BackendController;
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
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

public class S3Backend extends AbstractBackend {

  private static final Logger LOGGER = LogManager.getLogger(S3Backend.class);

  private static final String TOPIC_FILE_NAME = "julie-topics";
  private static final String SA_FILE_NAME = "julie-accounts";
  private static final String BINDINGS_FILE_NAME = "julie-bindings";

  private S3Client s3;
  private Configuration config;

  @Override
  public void configure(Configuration config) {
    configure(config, null, false);
  }

  // Visible and used for tests
  public void configure(Configuration config, URI endpoint, boolean anonymous) {
    this.config = config;
    S3ClientBuilder builder = S3Client.builder().region(Region.of(config.getS3Region()));
    if (endpoint != null) {
      builder = builder.endpointOverride(endpoint);
    }
    if (anonymous) {
      builder = builder.credentialsProvider(AnonymousCredentialsProvider.create());
    }
    this.s3 = builder.build();
  }

  @Override
  public void createOrOpen() {}

  @Override
  public void createOrOpen(BackendController.Mode mode) {}

  @Override
  public Set<ServiceAccount> loadServiceAccounts() throws IOException {
    try {
      String content = getRemoteStateContent(SA_FILE_NAME);
      return JSON.toSASet(content);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public Set<TopologyAclBinding> loadBindings() throws IOException {
    try {
      String content = getRemoteStateContent(BINDINGS_FILE_NAME);
      return JSON.toBindingsSet(content);
    } catch (IOException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public Set<String> loadTopics() throws IOException {
    try {
      String content = getRemoteStateContent(TOPIC_FILE_NAME);
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

  private void save(String content, String filename) {
    try {
      flushRemoteStateContent(content, filename);
    } catch (IOException e) {
      LOGGER.error(e);
    }
  }

  @Override
  public void close() {
    s3.close();
  }

  private String getRemoteStateContent(String key) throws IOException {
    GetObjectRequest request =
        GetObjectRequest.builder().key(key).bucket(config.getS3Bucket()).build();

    try {
      ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(request);
      return objectBytes.asString(StandardCharsets.UTF_8);
    } catch (S3Exception ex) {
      LOGGER.error(ex);
      throw new IOException(ex);
    }
  }

  private String flushRemoteStateContent(String content, String key) throws IOException {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(config.getS3Bucket()).key(key).build();
    try {
      PutObjectResponse response =
          s3.putObject(request, RequestBody.fromString(content, StandardCharsets.UTF_8));
      return response.eTag();
    } catch (S3Exception ex) {
      LOGGER.error(ex);
      throw new IOException(ex);
    }
  }
}
