package com.purbon.kafka.topology.actions.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.HashSet;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ClearAccountsTest {

  @Mock PrincipalProvider provider;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  public void shouldComposeDetailedViewOfProperties() {

    var accounts = new HashSet<ServiceAccount>();
    accounts.add(new ServiceAccount("1", "name", "description"));
    accounts.add(new ServiceAccount("1", "eman", "noitpircsed"));

    var action = new ClearAccounts(provider, accounts);
    var refs = action.refs();

    assertThat(refs).hasSize(2);
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"principal\" : \"name\",\n"
                + "  \"resource_name\" : \"rn://delete.account/com.purbon.kafka.topology.actions.accounts.ClearAccounts/name\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"principal\" : \"eman\",\n"
                + "  \"resource_name\" : \"rn://delete.account/com.purbon.kafka.topology.actions.accounts.ClearAccounts/eman\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");
  }
}
