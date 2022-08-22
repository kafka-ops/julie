package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.Configuration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceFilter {

  private static final Logger LOGGER = LogManager.getLogger(ResourceFilter.class);

  private final List<String> managedServiceAccountPrefixes;
  private final List<String> managedTopicPrefixes;
  private final List<String> managedGroupPrefixes;
  private final List<String> managedSubjectPrefixes;

  public ResourceFilter(Configuration config) {
    this.managedServiceAccountPrefixes = config.getServiceAccountManagedPrefixes();
    this.managedTopicPrefixes = config.getTopicManagedPrefixes();
    this.managedGroupPrefixes = config.getGroupManagedPrefixes();
    this.managedSubjectPrefixes = config.getSubjectManagedPrefixes();
  }

  public boolean matchesManagedPrefixList(TopologyAclBinding topologyAclBinding) {
    String resourceName = topologyAclBinding.getResourceName();
    String principle = topologyAclBinding.getPrincipal();
    // For global wild cards ACL's we manage only if we manage the service account/principle,
    // regardless. Filtering by service account will always take precedence if defined
    if (hasServiceAccountPrefixFilters() || resourceName.equals("*")) {
      if (resourceName.equals("*")) {
        return matchesServiceAccountPrefixList(principle);
      } else {
        return matchesServiceAccountPrefixList(principle)
            && matchesTopicOrSubjectOrGroupPrefix(topologyAclBinding, resourceName);
      }
    } else if (hasTopicNamePrefixFilter()
        || hasGroupNamePrefixFilter()
        || hasSubjectNamePrefixFilter()) {
      return matchesTopicOrSubjectOrGroupPrefix(topologyAclBinding, resourceName);
    }

    return true; // should include everything if not properly excluded earlier.
  }

  private boolean matchesTopicOrSubjectOrGroupPrefix(
      TopologyAclBinding topologyAclBinding, String resourceName) {
    if ("TOPIC".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
      return matchesTopicPrefixList(resourceName);
    } else if ("SUBJECT".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
      return matchesSubjectPrefixList(resourceName);
    } else if ("GROUP".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
      return matchesGroupPrefixList(resourceName);
    } else {
      // Nothing to filter out here
      return true;
    }
  }

  private boolean matchesTopicPrefixList(String topic) {
    return matchesPrefix(managedTopicPrefixes, topic, "Topic");
  }

  private boolean matchesGroupPrefixList(String group) {
    return matchesPrefix(managedGroupPrefixes, group, "Group");
  }

  private boolean matchesSubjectPrefixList(String subject) {
    return matchesPrefix(managedSubjectPrefixes, subject, "Subject");
  }

  private boolean matchesServiceAccountPrefixList(String principal) {
    return matchesPrefix(managedServiceAccountPrefixes, principal, "Principal");
  }

  private boolean hasServiceAccountPrefixFilters() {
    return managedServiceAccountPrefixes.size() != 0;
  }

  private boolean hasTopicNamePrefixFilter() {
    return managedTopicPrefixes.size() != 0;
  }

  private boolean hasGroupNamePrefixFilter() {
    return managedGroupPrefixes.size() != 0;
  }

  private boolean hasSubjectNamePrefixFilter() {
    return managedSubjectPrefixes.size() != 0;
  }

  private boolean matchesPrefix(List<String> prefixes, String item, String type) {
    boolean matches = prefixes.size() == 0 || prefixes.stream().anyMatch(item::startsWith);
    LOGGER.debug(String.format("%s %s matches %s with %s", type, item, matches, prefixes));
    return matches;
  }
}
