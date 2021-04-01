package com.purbon.kafka.topology.integration.backend;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.JULIE_S3_BUCKET;
import static com.purbon.kafka.topology.Constants.JULIE_S3_REGION;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.S3Backend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import io.findify.s3mock.S3Mock;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class S3BackendIT {

  private S3Mock api;
  private Map<String, String> cliOps;

  private static final String TEST_BUCKET = "testbucket";
  private static final String TEST_ENDPOINT = "http://127.0.0.1:8001";

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");

    String tmpDir = System.getProperty("java.io.tmpdir");
    Path s3Path = Paths.get(tmpDir, "s3");
    api = S3Mock.create(8001, s3Path.toFile().getAbsolutePath());
    api.start();

    AmazonS3Client client = new AmazonS3Client(new AnonymousAWSCredentials());
    client.setEndpoint(TEST_ENDPOINT);
    client.createBucket(TEST_BUCKET);
  }

  @After
  public void after() {
    api.shutdown();
  }

  @Test
  public void testContentCreation() throws IOException {

    S3Backend backend = new S3Backend();

    Properties props = new Properties();
    props.put(JULIE_S3_REGION, "us-west-2");
    props.put(JULIE_S3_BUCKET, TEST_BUCKET);

    Configuration config = new Configuration(cliOps, props);
    backend.configure(config, URI.create(TEST_ENDPOINT), true);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");

    backend.saveBindings(Collections.singleton(binding));
    backend.close();

    S3Backend newBackend = new S3Backend();
    newBackend.configure(config, URI.create(TEST_ENDPOINT), true);

    Set<TopologyAclBinding> newBindings = newBackend.loadBindings();
    assertThat(newBindings.size()).isEqualTo(1);
    assertThat(newBindings).contains(binding);
  }
}
