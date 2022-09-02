package com.purbon.kafka.topology.actions.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateAccountsTest {

  @Mock
  PrincipalProvider provider;

  @Test
  void shouldComposeDetailedViewOfProperties() {

    var accounts = new HashSet<ServiceAccount>();
    accounts.add(new ServiceAccount("1", "name", "description"));
    accounts.add(new ServiceAccount("1", "eman", "noitpircsed"));

    var action = new CreateAccounts(provider, accounts);
    var refs = action.refs();

    assertThat(refs).hasSize(2);
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"principal\" : \"name\",\n"
                + "  \"resource_name\" : \"rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/name\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");
    assertThat(refs)
        .contains(
            "{\n"
                + "  \"principal\" : \"eman\",\n"
                + "  \"resource_name\" : \"rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/eman\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");
  }
}
