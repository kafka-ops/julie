package kafka.ops.topology.actions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import kafka.ops.topology.PrincipalProvider;
import kafka.ops.topology.model.cluster.ServiceAccount;

public abstract class BaseAccountsAction extends BaseAction {

  protected PrincipalProvider provider;
  protected Collection<ServiceAccount> accounts;

  public BaseAccountsAction(PrincipalProvider provider, Collection<ServiceAccount> accounts) {
    this.provider = provider;
    this.accounts = accounts;
  }

  public Collection<ServiceAccount> getPrincipals() {
    return accounts;
  }

  @Override
  protected Map<String, Object> props() {
    Map<String, Object> map = new HashMap<>();
    map.put("Operation", getClass().getName());
    map.put("Principals", accounts);
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseAccountsAction)) {
      return false;
    }
    BaseAccountsAction that = (BaseAccountsAction) o;
    return Objects.equals(provider, that.provider) && Objects.equals(accounts, that.accounts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, accounts);
  }
}
