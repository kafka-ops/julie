package com.purbon.kafka.topology.audit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.purbon.kafka.topology.PrincipalProvider;
import com.purbon.kafka.topology.actions.accounts.CreateAccounts;
import com.purbon.kafka.topology.model.cluster.ServiceAccount;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditorTest {

  @Mock
  Appender appender;

  @Mock
  PrincipalProvider provider;

  @Test
  void shouldComposeDetailedViewOfProperties() {
    var accounts = new HashSet<ServiceAccount>();
    accounts.add(new ServiceAccount("1", "name", "description"));
    accounts.add(new ServiceAccount("1", "eman", "noitpircsed"));

    var action = new CreateAccounts(provider, accounts);

    var auditor = new Auditor(appender);
    auditor.log(action);
    verify(appender, times(1))
        .log(
            "{\n"
                + "  \"principal\" : \"name\",\n"
                + "  \"resource_name\" : \"rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/name\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");

    verify(appender, times(1))
        .log(
            "{\n"
                + "  \"principal\" : \"eman\",\n"
                + "  \"resource_name\" : \"rn://create.account/com.purbon.kafka.topology.actions.accounts.CreateAccounts/eman\",\n"
                + "  \"operation\" : \"com.purbon.kafka.topology.actions.BaseAccountsAction$1\"\n"
                + "}");
  }
}
