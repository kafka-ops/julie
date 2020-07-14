package server.api;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(
        title = "Kafka Topology Builder REST api",
        version = "0.1",
        description = "A REST api for Kafka Topology Builder",
        license = @License(name = "MIT License", url = "https://github.com/purbon/kafka-topology-builder/blob/master/LICENSE"),
        contact = @Contact(url = "https://github.com/purbon/kafka-topology-builder", name = "Pere Urbon", email = "pere.urbon@gmail.com")
    )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class);
    }
}
