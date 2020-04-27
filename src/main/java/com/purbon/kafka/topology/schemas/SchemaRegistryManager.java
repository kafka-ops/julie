package com.purbon.kafka.topology.schemas;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

import java.util.Collections;
import java.util.Optional;

public class SchemaRegistryManager {

    static class SchemaRegistryManagerException extends RuntimeException {
        public SchemaRegistryManagerException(String message) {
            super(message);
        }

        public SchemaRegistryManagerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final SchemaRegistryClient schemaRegistryClient;

    public SchemaRegistryManager(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    public int register(String subjectName, String schemaType, String schemaString) {
        final Optional<ParsedSchema> maybeSchema = schemaRegistryClient
                .parseSchema(schemaType, schemaString, Collections.emptyList());
        final ParsedSchema parsedSchema = maybeSchema.orElseThrow(() -> {
            final String msg = String.format("Failed to parse the schema for subject '%s' of type '%s'", subjectName, schemaType);
            return new SchemaRegistryManagerException(msg);
        });

        try {
            return schemaRegistryClient.register(subjectName, parsedSchema);
        } catch (Exception e) {
            final String msg = String.format("Failed to register the schema for subject '%s' of type '%s'", subjectName, schemaType);
            throw new SchemaRegistryManagerException(msg, e);
        }
    }
}
