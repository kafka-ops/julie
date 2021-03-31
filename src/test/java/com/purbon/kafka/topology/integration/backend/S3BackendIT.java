package com.purbon.kafka.topology.integration.backend;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;
import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.S3Backend;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import io.findify.s3mock.S3Mock;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class S3BackendIT {

    private S3Mock api;
    private Map<String, String> cliOps;

    private static final String TEST_BUCKET = "testbucket";

    @Before
    public void before() {
        cliOps = new HashMap<>();
        cliOps.put(BROKERS_OPTION, "");

        api = S3Mock.create(8001, "/tmp/s3");
        api.start();


        AmazonS3Client client = new AmazonS3Client(new AnonymousAWSCredentials());
        // use local API mock, not the AWS one
        client.setEndpoint("http://127.0.0.1:8001");
        client.createBucket(TEST_BUCKET);
        //client.putObject("testbucket", "file/name", "contents");


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
        backend.configure(config, URI.create("http://localhost:8001"));

        TopologyAclBinding binding =
                TopologyAclBinding.build(
                        ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");

        BackendState state = new BackendState();
        state.addBindings(Collections.singleton(binding));
        backend.save(state);
        backend.close();

        S3Backend newBackend = new S3Backend();
        newBackend.configure(config, URI.create("http://localhost:8001"));

        BackendState newState = newBackend.load();
        assertThat(newState.size()).isEqualTo(1);
        assertThat(newState.getBindings()).contains(binding);
    }
}

