package com.purbon.kafka.topology.actions.accounts;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.actions.BaseAccountsAction;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateAccounts extends BaseAccountsAction {

  private static final Logger LOGGER = LogManager.getLogger(CreateAccounts.class);

  public CreateAccounts(PrincipalProvider provider, Collection<ServiceAccount> accounts) {
    super(provider, accounts);
  }

  @Override
  public void run() throws IOException {
    LOGGER.debug("CreatePrincipals " + accounts);
    Set<ServiceAccount> mappedAccounts = new HashSet<>();
    for (ServiceAccount account : accounts) {
      ServiceAccount sa =
          provider.createServiceAccount(account.getName(), account.getDescription());
      mappedAccounts.add(sa);
    }
    accounts = mappedAccounts;
  }
}
