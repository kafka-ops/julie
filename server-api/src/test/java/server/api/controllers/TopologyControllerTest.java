package server.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.purbon.kafka.topology.model.Topology;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

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

        Topology topology = service.findByTeam("foo");
        assertEquals("foo", topology.getTeam());
    }

    @Test
    void testGetTopology() {

        HttpRequest createRequest = HttpRequest
            .POST("/topologies/foo", "");

        client
            .toBlocking()
            .exchange(createRequest);

        Topology topology = service.findByTeam("foo");
        assertEquals("foo", topology.getTeam());

        HttpRequest request = HttpRequest
            .GET("/topologies/foo");

        HttpResponse<Topology> response = client
            .toBlocking()
            .exchange(request);

        assertEquals(HttpStatus.OK, response.getStatus());

    }
}
