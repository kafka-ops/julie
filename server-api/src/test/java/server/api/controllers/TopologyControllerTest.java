package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import server.api.models.TopologyDeco;

@MicronautTest
public class TopologyControllerTest extends BaseControllerTest {

    @Test
    void testIndexOKResponse() {

        HttpRequest request = HttpRequest
            .GET("/topologies");
        HttpResponse response = client
            .toBlocking()
            .exchange(request);

        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testCreateTopology() {

        HttpRequest request = HttpRequest
            .POST("/topologies/foo", "");

        HttpResponse response = client
            .toBlocking()
            .exchange(request);

        assertEquals(HttpStatus.OK, response.getStatus());

        TopologyDeco topology = service.findByTeam("foo");
        assertEquals("foo", topology.getTeam());
    }

    @Test
    void testGetTopology() {

        HttpRequest createRequest = HttpRequest
            .POST("/topologies/foo", "");

        client
            .toBlocking()
            .exchange(createRequest);

        TopologyDeco topology = service.findByTeam("foo");
        assertEquals("foo", topology.getTeam());

        HttpRequest request = HttpRequest
            .GET("/topologies/foo");

        HttpResponse<TopologyDeco> response = client
            .toBlocking()
            .exchange(request);

        assertEquals(HttpStatus.OK, response.getStatus());

    }
}
