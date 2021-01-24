package kafka.ops.topology.actions.accounts;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import kafka.ops.topology.PrincipalProvider;
import kafka.ops.topology.actions.BaseAccountsAction;
import kafka.ops.topology.model.cluster.ServiceAccount;
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
