package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import server.api.services.GitManager;
import server.api.services.KafkaTopologyBuilderService;

import static org.mockito.Mockito.*;

@MicronautTest
public class WebookControllerTest {

    @Inject
    KafkaTopologyBuilderService builderService;

    @Inject
    GitManager gitService;

    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testPostResponse() throws URISyntaxException, IOException {

        doNothing().
            when(builderService)
            .sync(isA(String.class));

        when(gitService.cloneOrPull(isA(String.class), isA(String.class))).thenReturn("foo");

        URL payloadURL = getClass().getResource("/github-payload.json");
        List<String> lines = Files.readAllLines(Paths.get(payloadURL.toURI()));
        String payload = StringUtil.join("\n", lines).toString();

        HttpRequest request = HttpRequest
            .POST("/webhook", payload)
            .body(payload);

        HttpResponse response = client
            .toBlocking()
            .exchange(request);

        verify(builderService)
            .sync("foo/topologies/topology-for-cloud.yaml");
        verify(gitService)
            .cloneOrPull("https://github.com/purbon/kafka-topology-builder-demo.git",
                "kafka-topology-builder-demo");

        assertEquals(HttpStatus.OK, response.getStatus());
    }

    void testExceptionOnGitResponse() throws URISyntaxException, IOException {

        doNothing().
            when(builderService)
            .sync(isA(String.class));

        when(gitService.cloneOrPull(isA(String.class), isA(String.class))).thenThrow(IOException.class);

        URL payloadURL = getClass().getResource("/github-payload.json");
        List<String> lines = Files.readAllLines(Paths.get(payloadURL.toURI()));
        String payload = StringUtil.join("\n", lines).toString();

        HttpRequest request = HttpRequest
            .POST("/webhook", payload)
            .body(payload);

        HttpResponse response = client
            .toBlocking()
            .exchange(request);

        verify(builderService)
            .sync("foo/topologies/topology-for-cloud.yaml");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
    }

    @MockBean(KafkaTopologyBuilderService.class)
    KafkaTopologyBuilderService builderService() {
        return mock(KafkaTopologyBuilderService.class);
    }

    @MockBean(GitManager.class)
    GitManager gitService() {
        return mock(GitManager.class);
    }


}
