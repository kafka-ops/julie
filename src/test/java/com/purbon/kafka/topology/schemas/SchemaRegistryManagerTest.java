package com.purbon.kafka.topology.schemas;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.schemas.SchemaRegistryManager.SchemaRegistryManagerException;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

public class SchemaRegistryManagerTest {

  private static final String subjectName = "bananas";
  private static final String schemaType = "AVRO";
  private static final String simpleSchema = "{\"type\": \"string\"}";

  private SchemaRegistryClient client;
  private SchemaRegistryManager manager;

  @Before
  public void before() {
    client = new MockSchemaRegistryClient();
    Path rootDir = Paths.get(System.getProperty("user.dir"), "target", "test-classes");
    manager = new SchemaRegistryManager(client, rootDir.toString());
  }

  @Test
  public void shouldRegisterTheSchema() throws Exception {

    final int subjectId = manager.register(subjectName, schemaType, simpleSchema);
    assertThat(subjectId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test
  public void shouldRegisterTheSchemaWithDefaultAvroType() throws Exception {

    Path schemaFilePath =
        Paths.get(getClass().getClassLoader().getResource("schemas/bar-value.avsc").toURI());

    final int subjectId = manager.register(subjectName, schemaFilePath);
    assertThat(subjectId).isEqualTo(1);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(1).containsExactly(1);
  }

  @Test(expected = SchemaRegistryManagerException.class)
  public void shouldThrowAnExceptionWithFailedFilePath() {
    manager.register(subjectName, "schemas/wrong-file-value.avsc");
  }

  @Test
  public void shouldThrowAnExceptionWithValidRelativeFilePath() {
    manager.register(subjectName, "schemas/bar-value.avsc");
  }

  @Test
  public void shouldRegisterAndUpdateTheSchema() throws Exception {

    final String userSchema =
        "{\"type\":\"record\", \"name\":\"test\", "
            + "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"}]}";
    assertThat(manager.register(subjectName, schemaType, userSchema)).isEqualTo(1);

    final String updatedUserSchema =
        "{\"type\":\"record\", \"name\":\"test\", "
            + "\"fields\":[{\"name\":\"f1\",\"type\":\"string\"},{\"name\":\"f2\",\"type\":\"int\"}]}";
    assertThat(manager.register(subjectName, schemaType, updatedUserSchema)).isEqualTo(2);

    assertThat(client.getAllSubjects()).hasSize(1).containsExactly(subjectName);
    assertThat(client.getAllVersions(subjectName)).hasSize(2).containsExactly(1, 2);
  }

  @Test(expected = SchemaRegistryManager.SchemaRegistryManagerException.class)
  public void shouldFailForTheUnknownType() {

    final String unknownSchemaType = "bunch-of-monkeys";
    manager.register(subjectName, unknownSchemaType, simpleSchema);
  }
}
