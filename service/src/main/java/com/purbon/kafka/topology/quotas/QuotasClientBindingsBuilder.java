package com.purbon.kafka.topology.quotas;

import com.purbon.kafka.topology.model.users.Quota;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;

public class QuotasClientBindingsBuilder {

  private static final String CONSUMER_BYTE_RATE = "consumer_byte_rate";
  private static final String PRODUCER_BYTE_RATE = "producer_byte_rate";
  private static final String REQUEST_RATE = "request_percentage";

  Quota quota = null;

  public QuotasClientBindingsBuilder(Quota quota) {
    this.quota = quota;
  }

  private ClientQuotaEntity buildClient() {
    Map<String, String> entries = new HashMap<>();
    entries.put(ClientQuotaEntity.USER, null);
    if (!quota.getPrincipal().isEmpty()) {
      entries.put(ClientQuotaEntity.USER, quota.getPrincipal());
    }
    return new ClientQuotaEntity(entries);
  }

  private ClientQuotaAlteration.Op addConsumer() {
    return new ClientQuotaAlteration.Op(
        CONSUMER_BYTE_RATE, quota.getConsumer_byte_rate().orElse(null));
  }

  private ClientQuotaAlteration.Op addProducer() {
    return new ClientQuotaAlteration.Op(
        PRODUCER_BYTE_RATE, quota.getProducer_byte_rate().orElse(null));
  }

  private ClientQuotaAlteration.Op addRequestRate() {
    return new ClientQuotaAlteration.Op(REQUEST_RATE, quota.getRequest_percentage().orElse(null));
  }

  public ClientQuotaAlteration build() {
    ClientQuotaEntity entityQuota = buildClient();
    List ops = new ArrayList();
    CollectNotNull.addSafe(ops, addConsumer(), addProducer(), addRequestRate());
    return new ClientQuotaAlteration(entityQuota, ops);
  }

  public static final class CollectNotNull {
    public static <T> boolean addSafe(List<T> list, T... elements) {
      if (list == null) return false;
      for (T element : elements) {
        if (element != null) list.add(element);
      }
      return true;
    }
  }
}
