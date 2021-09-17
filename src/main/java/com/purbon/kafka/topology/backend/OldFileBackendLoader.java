package com.purbon.kafka.topology.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import com.purbon.kafka.topology.utils.JSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class OldFileBackendLoader {

  private static final Logger LOGGER = LogManager.getLogger(OldFileBackendLoader.class);
  static final String SERVICE_ACCOUNTS_TAG = "ServiceAccounts";
  static final String TOPICS_TAG = "Topics";
  static final String ACLS_TAG = "acls";

  BackendState load(File file) throws IOException {
    BackendState state = new BackendState();
    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
      String type = null;
      String line;
      while ((line = in.readLine()) != null) {
        if (type == null || isControlTag(line)) {
          type = line;
          continue;
        }
        if (type.equalsIgnoreCase(SERVICE_ACCOUNTS_TAG)) {
          final ServiceAccount serviceAccount = parseServiceAccount(line);
          if (serviceAccount != null) {
            state.addAccounts(Collections.singleton(serviceAccount));
          }
        } else if (type.equalsIgnoreCase(ACLS_TAG)) {
          state.addBindings(Collections.singleton(parseAcl(line)));
        } else if (type.equalsIgnoreCase(TOPICS_TAG)) {
          state.addTopics(Collections.singleton(parseTopic(line)));
        } else {
          throw new IOException("Binding type \"" + type + "\" not supported.");
        }
      }
    }
    return state;
  }

  private ServiceAccount parseServiceAccount(final String line) {
    try {
      return (ServiceAccount) JSON.toObject(line, ServiceAccount.class);
    } catch (JsonProcessingException e) {
      LOGGER.error(e);
      return null;
    }
  }

  private TopologyAclBinding parseAcl(final String line) throws IOException {
    return BackendHelper.buildAclBinding(line);
  }

  private String parseTopic(final String line) {
    return line.trim();
  }

  static boolean isControlTag(String line) {
    return line.equalsIgnoreCase(SERVICE_ACCOUNTS_TAG)
        || line.equalsIgnoreCase(TOPICS_TAG)
        || line.equalsIgnoreCase(ACLS_TAG);
  }
}
