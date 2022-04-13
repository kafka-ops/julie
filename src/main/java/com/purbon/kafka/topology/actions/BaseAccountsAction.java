package com.purbon.kafka.topology.actions;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  protected List<Map<String, Object>> detailedProps() {
    return accounts.stream()
        .map(
            new Function<ServiceAccount, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(ServiceAccount account) {
                Map<String, Object> map = new HashMap<>();
                map.put("resource_name", resourceNameBuilder(account));
                map.put("operation", getClass().getName());
                map.put("principal", account.getName());
                return map;
              }
            })
        .collect(Collectors.toList());
  }

  protected abstract String resourceNameBuilder(ServiceAccount account);

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
