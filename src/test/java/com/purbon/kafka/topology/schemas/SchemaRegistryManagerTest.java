package com.purbon.kafka.topology.schemas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.schemas.SchemaRegistryManager.SchemaRegistryManagerException;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SchemaRegistryManagerTest {

  private static final String subjectName = "bananas";
  private static final String schemaType = "AVRO";
  private static final String simpleSchema = "{\"type\": \"string\"}";

  private SchemaRegistryClient client;
  private SchemaRegistryManager manager;

  private Path rootDir;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SchemaRegistryClient mockClient;

  @Before
  public void before() {
    client = new MockSchemaRegistryClient();
    rootDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");
    manager = new SchemaRegistryManager(client, rootDir.toString());
  }

  @Test
  public void shouldRegisterTheSchema() throws Exception {

    final int subjectId = manager.save(subjectName, schemaType, simpleSchema);
    assertThat(subjectId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test
  public void shouldRegisterTheSchemaWithDefaultAvroType() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/bar-value.avsc").toURI());

    final int subjectId = manager.register(subjectName, schemaFilePath, AvroSchema.TYPE);
    assertThat(subjectId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test
  public void shouldRegisterTheSchemawithCompatibility()
      throws IOException, RestClientException, URISyntaxException {
    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/bar-value.avsc").toURI());

    final int subjectId = manager.register(subjectName, schemaFilePath, AvroSchema.TYPE);
    assertThat(subjectId).isEqualTo(1);

    String compLevel = manager.setCompatibility(subjectName, "FORWARD");
    assertThat(compLevel).isEqualTo("FORWARD");
    assertThat(client.getCompatibility(subjectName)).isEqualTo("FORWARD");
  }

  @Test(expected = SchemaRegistryManagerException.class)
  public void shouldThrowAnExceptionWithFailedFilePath() {
    manager.register(subjectName, "schemas/wrong-file-value.avsc", AvroSchema.TYPE);
  }

  @Test
  public void shouldRegisterSchemasWithARelativePath() {
    SchemaRegistryManager managerSpy = Mockito.spy(manager);
    managerSpy.register(subjectName, "schemas/bar-value.avsc", AvroSchema.TYPE);
    Path mayBeAbsolutePath = Paths.get(rootDir.toString(), "schemas/bar-value.avsc");
    verify(managerSpy, times(1)).register(subjectName, mayBeAbsolutePath, AvroSchema.TYPE);
  }

  @Test
  public void shouldRegisterSchemasWithAnAbsolutePath() {
    SchemaRegistryManager managerSpy = Mockito.spy(manager);
    Path mayBeAbsolutePath = Paths.get(rootDir.toString(), "schemas/bar-value.avsc");
    managerSpy.register(subjectName, mayBeAbsolutePath.toString(), AvroSchema.TYPE);
    verify(managerSpy, times(1)).register(subjectName, mayBeAbsolutePath, AvroSchema.TYPE);
  }

  @Test
  public void shouldRegisterAndUpdateTheSchema() throws Exception {

    final String userSchema =
        "{\"type\":\"record\", \"name\":\"test\", "
            + "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"}]}";
    assertThat(manager.save(subjectName, schemaType, userSchema)).isEqualTo(1);

    final String updatedUserSchema =
        "{\"type\":\"record\", \"name\":\"test\", "
            + "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"},{\"name\":\"f2\",\"type\":\"int\"}]}";
    assertThat(manager.save(subjectName, schemaType, updatedUserSchema)).isEqualTo(2);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
  }

  @Test(expected = SchemaRegistryManager.SchemaRegistryManagerException.class)
  public void shouldFailForTheUnknownType() {

    final String unknownSchemaType = "bunch-of-monkeys";
    manager.register(subjectName, unknownSchemaType, simpleSchema);
  }
}
