package kafka.ops.topology;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import kafka.ops.topology.api.adminclient.TopologyBuilderAdminClient;
import kafka.ops.topology.api.mds.MdsApiClient;
import kafka.ops.topology.api.mds.MdsApiClientBuilder;
import kafka.ops.topology.roles.RbacProvider;
import kafka.ops.topology.roles.SimpleAclsProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlProviderFactoryTest {

  @Mock TopologyBuilderAdminClient adminClient;

  @Mock MdsApiClientBuilder mdsApiClientBuilder;

  @Mock MdsApiClient mdsApiClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  Map<String, String> cliOps;
  Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  public void testRBACConfig() throws IOException {

    props.put(
        TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS,
        "kafka.ops.topology.roles.RBACProvider");
    props.put(TopologyBuilderConfig.MDS_SERVER, "http://localhost:8090");
    props.put(TopologyBuilderConfig.MDS_USER_CONFIG, "alice");
    props.put(TopologyBuilderConfig.MDS_PASSWORD_CONFIG, "alice-secret");
    props.put(TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG, "UtBZ3rTSRtypmmkAL1HbHw");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    when(mdsApiClientBuilder.build()).thenReturn(mdsApiClient);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);

    AccessControlProvider provider = factory.get();

    verify(mdsApiClient, times(1)).login("alice", "alice-secret");
    verify(mdsApiClient, times(1)).authenticate();

    assertThat(provider, instanceOf(RbacProvider.class));
  }

  @Test
  public void testACLsConfig() throws IOException {

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);

    assertThat(factory.get(), instanceOf(SimpleAclsProvider.class));
  }

  @Test(expected = IOException.class)
  public void testWrongProviderConfig() throws IOException {

    props.put(
        TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS,
        "kafka.ops.topology.roles.MyCustomProvider");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    when(mdsApiClientBuilder.build()).thenReturn(mdsApiClient);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);
    factory.get();
  }
}
