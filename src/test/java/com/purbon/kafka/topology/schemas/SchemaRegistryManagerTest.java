package com.purbon.kafka.topology.schemas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.schemas.SchemaRegistryManager.SchemaRegistryManagerException;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaRegistryManagerTest {

  private static final String subjectName = "bananas";
  private static final String schemaTypeAvro = "AVRO";
  private static final String schemaTypeJson = "JSON";
  private static final String schemaTypeProtobuf = "PROTOBUF";
  private static final String simpleSchema = "{\"type\": \"string\"}";

  private SchemaRegistryClient client;
  private SchemaRegistryManager manager;

  private Path rootDir;

  @BeforeEach
  void before() {
    List<SchemaProvider> providers =
        Arrays.asList(
            new AvroSchemaProvider(), new JsonSchemaProvider(), new ProtobufSchemaProvider());
    client = new MockSchemaRegistryClient(providers);
    rootDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");
    manager = new SchemaRegistryManager(client, rootDir.toString());
  }

  @Test
  void shouldRegisterTheSchema() throws Exception {

    final int schemaId = manager.save(subjectName, schemaTypeAvro, simpleSchema);
    assertThat(schemaId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test
  void shouldRegisterTheSchemaWithDefaultAvroType() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/bar-value.avsc").toURI());

    final int schemaId = manager.register(subjectName, schemaFilePath, AvroSchema.TYPE);
    assertThat(schemaId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test
  void shouldRegisterTheSchemawithCompatibility()
      throws IOException, RestClientException, URISyntaxException {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/bar-value.avsc").toURI());

    final int schemaId = manager.register(subjectName, schemaFilePath, AvroSchema.TYPE);
    assertThat(schemaId).isEqualTo(1);

    String compLevel = manager.setCompatibility(subjectName, "FORWARD");
    assertThat(compLevel).isEqualTo("FORWARD");
    assertThat(client.getCompatibility(subjectName)).isEqualTo("FORWARD");
  }

  @Test
  void shouldThrowAnExceptionWithFailedFilePath() {
    assertThrows(
        SchemaRegistryManagerException.class,
        () -> {
          manager.register(subjectName, "schemas/wrong-file-value.avsc", AvroSchema.TYPE);
        });
  }

  @Test
  void shouldRegisterSchemasWithARelativePath() {
    SchemaRegistryManager managerSpy = Mockito.spy(manager);
    managerSpy.register(subjectName, "schemas/bar-value.avsc", AvroSchema.TYPE);
    Path mayBeAbsolutePath = Paths.get(rootDir.toString(), "schemas/bar-value.avsc");
    verify(managerSpy, times(1)).register(subjectName, mayBeAbsolutePath, AvroSchema.TYPE);
  }

  @Test
  void shouldRegisterSchemasWithAnAbsolutePath() {
    SchemaRegistryManager managerSpy = Mockito.spy(manager);
    Path mayBeAbsolutePath = Paths.get(rootDir.toString(), "schemas/bar-value.avsc");
    managerSpy.register(subjectName, mayBeAbsolutePath.toString(), AvroSchema.TYPE);
    verify(managerSpy, times(1)).register(subjectName, mayBeAbsolutePath, AvroSchema.TYPE);
  }

  @Test
  void shouldRegisterAndUpdateAvroSchema() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.avsc").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeAvro, sampleSchema)).isEqualTo(1);

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-backward-compatible.avsc")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema persedUpdatedUserSchema =
        client.parseSchema(schemaTypeAvro, updatedSampleSchema, Collections.emptyList()).get();
    assertThat(client.testCompatibility(subjectName, persedUpdatedUserSchema)).isTrue();

    assertThat(manager.save(subjectName, schemaTypeAvro, updatedSampleSchema)).isEqualTo(2);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
  }

  @Test
  void shouldDetectIncompatibleAvroSchema()
      throws URISyntaxException, IOException, RestClientException {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.avsc").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeAvro, sampleSchema)).isEqualTo(1);
    manager.setCompatibility(subjectName, "FORWARD");
    assertThat(client.getCompatibility(subjectName)).isEqualTo("FORWARD");

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-backward-compatible.avsc")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema parsedUpdatedSampleSchema =
        client.parseSchema(schemaTypeAvro, updatedSampleSchema, Collections.emptyList()).get();
    assertThat(client.testCompatibility(subjectName, parsedUpdatedSampleSchema)).isFalse();
  }

  @Test
  void shouldRegisterAndUpdateJsonSchema() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.json").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeJson, sampleSchema)).isEqualTo(1);

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-forward-compatible.json")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema parsedUpdatedSampleSchema =
        client.parseSchema(schemaTypeJson, updatedSampleSchema, Collections.emptyList()).get();

    assertThat(client.testCompatibility(subjectName, parsedUpdatedSampleSchema)).isTrue();

    assertThat(manager.save(subjectName, schemaTypeJson, updatedSampleSchema)).isEqualTo(2);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
  }

  @Test
  void shouldDetectIncompatibleJsonSchema()
      throws URISyntaxException, IOException, RestClientException {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.json").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeJson, sampleSchema)).isEqualTo(1);

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-backward-compatible.json")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema parsedUpdatedSampleSchema =
        client.parseSchema(schemaTypeJson, updatedSampleSchema, Collections.emptyList()).get();
    assertThat(client.testCompatibility(subjectName, parsedUpdatedSampleSchema)).isFalse();
  }

  @Test
  void shouldRegisterAndUpdateProtobufSchema() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.proto").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeProtobuf, sampleSchema)).isEqualTo(1);

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-backward-compatible.proto")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema parsedUpdatedSampleSchema =
        client.parseSchema(schemaTypeProtobuf, updatedSampleSchema, Collections.emptyList()).get();
    assertThat(client.testCompatibility(subjectName, parsedUpdatedSampleSchema)).isTrue();

    assertThat(manager.save(subjectName, schemaTypeProtobuf, updatedSampleSchema)).isEqualTo(2);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
  }

  @Test
  void shouldDetectIncompatibleProtobufSchema()
      throws URISyntaxException, IOException, RestClientException {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/test.proto").toURI());
    final String sampleSchema = new String(Files.readAllBytes(schemaFilePath));
    assertThat(manager.save(subjectName, schemaTypeProtobuf, sampleSchema)).isEqualTo(1);

    Path updatedSchemaFilePath =
        Paths.get(
            getClass()
                .getClassLoader()
                .getResource("schemas/test-forward-compatible.proto")
                .toURI());
    final String updatedSampleSchema = new String(Files.readAllBytes(updatedSchemaFilePath));

    final ParsedSchema parsedUpdatedSampleSchema =
        client.parseSchema(schemaTypeProtobuf, updatedSampleSchema, Collections.emptyList()).get();
    assertThat(client.testCompatibility(subjectName, parsedUpdatedSampleSchema)).isFalse();
  }

  @Test
  void shouldFailForTheUnknownType() {
    assertThrows(
        SchemaRegistryManager.SchemaRegistryManagerException.class,
        () -> {
          final String unknownSchemaType = "bunch-of-monkeys";
          manager.register(subjectName, unknownSchemaType, simpleSchema);
        });
  }
}
