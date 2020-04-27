package com.purbon.kafka.topology.schemas;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaRegistryManagerTest {

    private static final String subjectName = "bananas";
    private static final String schemaType = "AVRO";
    private static final String simpleSchema = "{\"type\": \"string\"}";

    @Test
    public void shouldRegisterTheSchema() throws Exception {
        final SchemaRegistryClient client = new MockSchemaRegistryClient();
        final SchemaRegistryManager manager = new SchemaRegistryManager(client);

        final int subjectId = manager.register(subjectName, schemaType, simpleSchema);
        assertThat(subjectId).isEqualTo(1);

        assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
        assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
    }

    @Test
    public void shouldRegisterAndUpdateTheSchema() throws Exception {
        final SchemaRegistryClient client = new MockSchemaRegistryClient();
        final SchemaRegistryManager manager = new SchemaRegistryManager(client);

        final String userSchema = "{\"type\":\"record\", \"name\":\"test\", " +
                "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"}]}";
        assertThat(manager.register(subjectName, schemaType, userSchema)).isEqualTo(1);

        final String updatedUserSchema = "{\"type\":\"record\", \"name\":\"test\", " +
                "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"},{\"name\":\"f2\",\"type\":\"int\"}]}";
        assertThat(manager.register(subjectName, schemaType, updatedUserSchema)).isEqualTo(2);

        assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
        assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
    }

    @Test(expected = SchemaRegistryManager.SchemaRegistryManagerException.class)
    public void shouldFailForTheUnknownType() {
        final SchemaRegistryClient client = new MockSchemaRegistryClient();
        final SchemaRegistryManager manager = new SchemaRegistryManager(client);

        final String unknownSchemaType = "bunch-of-monkeys";
        manager.register(subjectName, unknownSchemaType, simpleSchema);
    }
}
