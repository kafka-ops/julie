package com.purbon.kafka.topology.actions.accounts;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.actions.BaseAccountsAction;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.io.IOException;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClearAccounts extends BaseAccountsAction {

  private static final Logger LOGGER = LogManager.getLogger(ClearAccounts.class);

  public ClearAccounts(PrincipalProvider provider, Collection<ServiceAccount> accounts) {
    super(provider, accounts);
  }

  @Override
  public void run() throws IOException {
    LOGGER.debug("ClearPrincipals " + accounts);
    for (ServiceAccount account : accounts) {
      provider.deleteServiceAccount(account.getName());
    }
  }
}
