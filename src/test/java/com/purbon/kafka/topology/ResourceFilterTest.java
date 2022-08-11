package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.SERVICE_ACCOUNT_MANAGED_PREFIXES;
import static com.purbon.kafka.topology.Constants.TOPIC_MANAGED_PREFIXES;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.roles.ResourceFilter;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.HashMap;
import java.util.Properties;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Test;

public class ResourceFilterTest {

  @Test
  public void resourcesWithPrincipalAndNameMatchesShouldBeFilter() {
    Configuration config = makeConfig(new String[] {"User:foo"}, new String[] {"d.foo.bar"});
    ResourceFilter filter = new ResourceFilter(config);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "d.foo.bar", "*", "read", "User:foo", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isTrue();

    binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "d.foo.bar", "*", "read", "User:bar", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isFalse();

    binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "c.f.b", "*", "read", "User:foo", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isFalse();
  }

  @Test
  public void resourcesWithPrincipalMatchesShouldBeFiltered() {
    Configuration config = makeConfig(new String[] {"User:foo"});
    ResourceFilter filter = new ResourceFilter(config);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "d.foo.bar", "*", "read", "User:foo", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isTrue();
  }

  @Test
  public void resourcesWithNamesMatchesShouldBeFiltered() {
    Configuration config = makeConfigOnlyNames(new String[] {"d.foo.bar"});
    ResourceFilter filter = new ResourceFilter(config);

    TopologyAclBinding binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "d.foo.bar", "*", "read", "User:foo", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isTrue();

    binding =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "c.foo.zet", "*", "read", "User:foo", "PREFIXED");
    assertThat(filter.matchesManagedPrefixList(binding)).isFalse();
  }

  private Configuration makeConfigOnlyNames(String[] names) {
    return makeConfig(new String[0], names);
  }

  private Configuration makeConfig(String[] principals) {
    return makeConfig(principals, new String[0]);
  }

  private Configuration makeConfig(String[] principals, String[] names) {
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    Properties props = new Properties();

    for (int i = 0; i < principals.length; i++) {
      String key = String.format("%s.%d", SERVICE_ACCOUNT_MANAGED_PREFIXES, i);
      props.put(key, principals[i]);
    }

    for (int i = 0; i < names.length; i++) {
      String key = String.format("%s.%d", TOPIC_MANAGED_PREFIXES, i);
      props.put(key, names[i]);
    }

    return new Configuration(cliOps, props);
  }
}
