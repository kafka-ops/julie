package com.purbon.kafka.topology.backend;

import static com.purbon.kafka.topology.Constants.STATE_FILE_NAME;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.utils.JSON;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GCPBackend implements Backend {

  private static final Logger LOGGER = LogManager.getLogger(GCPBackend.class);

  private Storage storage;
  private Configuration config;

  @Override
  public void configure(Configuration config) {
    configure(config, null);
  }

  public void configure(Configuration config, URI endpoint) {
    this.config = config;
    this.storage =
        StorageOptions.newBuilder().setProjectId(config.getGCPProjectId()).build().getService();
  }

  @Override
  public void save(BackendState state) throws IOException {
    BlobId blobId = BlobId.of(config.getGCPBucket(), STATE_FILE_NAME);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    try {
      storage.create(
          blobInfo,
          state.asJson().getBytes(StandardCharsets.UTF_8),
          Storage.BlobTargetOption.detectContentType());
    } catch (Exception ex) {
      LOGGER.error(ex);
      throw new IOException(ex);
    }
  }

  @Override
  public BackendState load() throws IOException {
    try {
      Blob blob = storage.get(BlobId.of(config.getGCPBucket(), STATE_FILE_NAME));
      String contentJson = new String(blob.getContent(), StandardCharsets.UTF_8);
      return (BackendState) JSON.toObject(contentJson, BackendState.class);
    } catch (Exception ex) {
      LOGGER.error(ex);
      throw new IOException(ex);
    }
  }

  @Override
  public void close() {
    // empty
  }
}
