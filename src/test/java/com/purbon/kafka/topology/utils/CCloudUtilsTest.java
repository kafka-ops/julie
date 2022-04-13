package com.purbon.kafka.topology.utils;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.ccloud.CCloudApi;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.model.cluster.ServiceAccountV1;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CCloudUtilsTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Mock CCloudApi cCloudApi;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @After
  public void after() {}

  @Test
  public void translationShouldNotRaiseErrors() throws IOException {
    Configuration config = new Configuration(cliOps, props);
    var utils = new CCloudUtils(config);
    testTranslationMechanism(utils, "sa-xxxx", "User:foo");
  }

  @Test
  public void translationShouldNotRaiseErrorWhenNotUsingUserPrefix() throws IOException {
    Configuration config = new Configuration(cliOps, props);
    var utils = new CCloudUtils(config);
    testTranslationMechanism(utils, "sa-xxxx", "foo");
  }

  private void testTranslationMechanism(CCloudUtils utils, String resourceId, String serviceName) throws IOException {

    var accounts = new HashSet<>();
    accounts.add(new ServiceAccount(resourceId, serviceName, "description", resourceId));
    doReturn(accounts).when(cCloudApi).listServiceAccounts();

    var accountsV1 = new HashSet<>();
    accountsV1.add(new ServiceAccountV1(12345L, "email", serviceName, resourceId));
    doReturn(accountsV1).when(cCloudApi).listServiceAccountsV1();

    var lookupTable = utils.initializeLookupTable(cCloudApi);

    TopologyAclBinding binding =
            TopologyAclBinding.build(
                    ResourceType.CLUSTER.name(),
                    "resourceName",
                    "host",
                    "operation",
                    serviceName,
                    "pattern");

    var translatedBinding = utils.translateIfNecessary(binding, lookupTable);

    assertThat(translatedBinding.getPrincipal()).isEqualTo("User:12345");
  }

  @Test(expected = IOException.class)
  public void translationShouldBeAbortedIfErrors() throws IOException {
    Configuration config = new Configuration(cliOps, props);
    var utils = new CCloudUtils(config);
    doThrow(new IOException()).when(cCloudApi).listServiceAccounts();
    utils.initializeLookupTable(cCloudApi);
  }
}
