package com.purbon.kafka.topology.serdes;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookupFactory;

/**
 * A simple substitutor which substitutes system properties in the topology file. If a property is
 * not found, it throws a {@link com.purbon.kafka.topology.exceptions.TopologyParsingException}.
 */
public class SystemPropertySubstitutor {
  public SystemPropertySubstitutor() {
  }

  public String replace(String original) {
    try {
      return new StringSubstitutor(StringLookupFactory.INSTANCE.systemPropertyStringLookup())
        .setEnableUndefinedVariableException(true)
        .replace(original);
    } catch (IllegalArgumentException ex) {
      throw new TopologyParsingException("A variable was not resolved: ", ex);
    }
  }
}
