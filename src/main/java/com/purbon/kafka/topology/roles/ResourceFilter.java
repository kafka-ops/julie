package com.purbon.kafka.topology.roles;

import com.purbon.kafka.topology.AccessControlManager;
import com.purbon.kafka.topology.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ResourceFilter {

    private static final Logger LOGGER = LogManager.getLogger(ResourceFilter.class);

    private final List<String> managedServiceAccountPrefixes;
    private final List<String> managedTopicPrefixes;
    private final List<String> managedGroupPrefixes;

    public ResourceFilter(Configuration config) {
        this.managedServiceAccountPrefixes = config.getServiceAccountManagedPrefixes();
        this.managedTopicPrefixes = config.getTopicManagedPrefixes();
        this.managedGroupPrefixes = config.getGroupManagedPrefixes();
    }

    public boolean matchesManagedPrefixList(TopologyAclBinding topologyAclBinding) {
        String resourceName = topologyAclBinding.getResourceName();
        String principle = topologyAclBinding.getPrincipal();
        // For global wild cards ACL's we manage only if we manage the service account/principle,
        // regardless. Filtering by service account will always take precedence if defined
        if (haveServiceAccountPrefixFilters() || resourceName.equals("*")) {
            if (resourceName.equals("*")) {
                return matchesServiceAccountPrefixList(principle);
            } else {
               return matchesServiceAccountPrefixList(principle)
                       && matchesTopicOrGroupPrefix(topologyAclBinding, resourceName);
            }
        } else if (haveTopicNamePrefixFilter() || haveGroupNamePrefixFilter()) {
            return matchesTopicOrGroupPrefix(topologyAclBinding, resourceName);
        }

        return true; // should include everything if not properly excluded earlier.
    }

    private boolean matchesTopicOrGroupPrefix(
            TopologyAclBinding topologyAclBinding, String resourceName) {
        if ("TOPIC".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
            return matchesTopicPrefixList(resourceName);
        } else if ("GROUP".equalsIgnoreCase(topologyAclBinding.getResourceType())) {
            return matchesGroupPrefixList(resourceName);
        }
        return false;
    }

    private boolean matchesTopicPrefixList(String topic) {
        return matchesPrefix(managedTopicPrefixes, topic, "Topic");
    }

    private boolean matchesGroupPrefixList(String group) {
        return matchesPrefix(managedGroupPrefixes, group, "Group");
    }

    private boolean matchesServiceAccountPrefixList(String principal) {
        return matchesPrefix(managedServiceAccountPrefixes, principal, "Principal");
    }

    private boolean haveServiceAccountPrefixFilters() {
        return managedServiceAccountPrefixes.size() != 0;
    }

    private boolean haveTopicNamePrefixFilter() {
        return managedTopicPrefixes.size() != 0;
    }

    private boolean haveGroupNamePrefixFilter() {
        return managedGroupPrefixes.size() != 0;
    }

    private boolean matchesPrefix(List<String> prefixes, String item, String type) {
        boolean matches = prefixes.size() == 0 || prefixes.stream().anyMatch(item::startsWith);
        LOGGER.debug(String.format("%s %s matches %s with $s", type, item, matches, prefixes));
        return matches;
    }
}
