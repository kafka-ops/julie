package com.purbon.kafka.topology.backend;

import com.purbon.kafka.topology.BackendController;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

public class S3Backend extends AbstractBackend {

  private static final Logger LOGGER = LogManager.getLogger(S3Backend.class);
  public static final String STATE_FILE_NAME = ".cluster-state";

  private S3Client s3;
  private Configuration config;

  public void configure(Configuration config) {
    configure(config, null);
  }
  public void configure(Configuration config, URI endpoint) {
    this.config = config;
    S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(config.getS3Region()));
    if (endpoint != null) {
      builder = builder.endpointOverride(endpoint);
    }
    this.s3 = builder.build();
  }

  @Override
  public void createOrOpen() {}

  @Override
  public void createOrOpen(BackendController.Mode mode) {}

  @Override
  public void saveType(String type) {}

  @Override
  public void save(BackendState state) throws IOException {
    flushRemoteStateContent(state.asJson(), STATE_FILE_NAME);
  }

  @Override
  public BackendState load() throws IOException {
    try {
      String content = getRemoteStateContent(STATE_FILE_NAME);
      return (BackendState) JSON.toObject(content, BackendState.class);
    } catch (IOException ex) {
      LOGGER.debug(ex);
      return new BackendState();
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
