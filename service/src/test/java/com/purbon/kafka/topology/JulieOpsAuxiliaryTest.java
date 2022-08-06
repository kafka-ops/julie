package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.audit.KafkaAppender;
import com.purbon.kafka.topology.audit.StdoutAppender;
import com.purbon.kafka.topology.backend.FileBackend;
import com.purbon.kafka.topology.backend.GCPBackend;
import com.purbon.kafka.topology.backend.KafkaBackend;
import com.purbon.kafka.topology.backend.RedisBackend;
import com.purbon.kafka.topology.backend.S3Backend;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.KafkaException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JulieOpsAuxiliaryTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
    props.put(JULIE_AUDIT_ENABLED, "true");
  }

  @After
  public void after() {}

  @Test
  public void shouldConfigureAFileBackend() throws IOException {
    testBackend(STATE_PROCESSOR_DEFAULT_CLASS, FileBackend.class);
  }

  @Test
  public void shouldConfigureARedisBackend() throws IOException {
    testBackend(REDIS_STATE_PROCESSOR_CLASS, RedisBackend.class);
  }

  @Test
  public void shouldConfigureAS3Backend() throws IOException {
    props.put(JULIE_S3_REGION, "region");
    testBackend(S3_STATE_PROCESSOR_CLASS, S3Backend.class);
  }

  @Test
  public void shouldConfigureAGCPBackend() throws IOException {
    props.put(JULIE_GCP_BUCKET, "bucket");
    props.put(JULIE_GCP_PROJECT_ID, "project");
    testBackend(GCP_STATE_PROCESSOR_CLASS, GCPBackend.class);
  }

  @Test(expected = KafkaException.class)
  public void shouldConfigureAKafkaBackend() throws IOException {
    testBackend(KAFKA_STATE_PROCESSOR_CLASS, KafkaBackend.class);
  }

  @Test
  public void shouldConfigureAnStdoutAuditor() throws IOException {
    testAuditor("com.purbon.kafka.topology.audit.StdoutAppender", StdoutAppender.class);
  }

  @Test
  public void shouldConfigureAKafkaAuditor() throws IOException {
    testAuditor("com.purbon.kafka.topology.audit.KafkaAppender", KafkaAppender.class);
  }

  private void testAuditor(String appenderName, Class appenderClass) throws IOException {
    props.put(JULIE_AUDIT_APPENDER_CLASS, appenderName);
    Configuration config = new Configuration(cliOps, props);
    var auditor = JulieOpsAuxiliary.configureAndBuildAuditor(config);
    assertThat(auditor.getAppender()).isInstanceOf(appenderClass);
  }

  private void testBackend(String processorClass, Class backendClass) throws IOException {
    props.put(STATE_PROCESSOR_IMPLEMENTATION_CLASS, processorClass);
    Configuration config = new Configuration(cliOps, props);
    var backend = JulieOpsAuxiliary.buildBackendController(config);
    assertThat(backend.getBackend()).isInstanceOf(backendClass);
  }
}
