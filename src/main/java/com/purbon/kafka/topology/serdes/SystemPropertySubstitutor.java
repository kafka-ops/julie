package com.purbon.kafka.topology.serdes;

import com.purbon.kafka.topology.exceptions.TopologyParsingException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A simple substitutor which substitutes system properties in the topology file.
 * If a property is not found, it throws a {@link com.purbon.kafka.topology.exceptions.TopologyParsingException}.
 */
public class SystemPropertySubstitutor {
  private final Map<String, String> env;
  private final StrSubstitutor strSubstitutor;

  public SystemPropertySubstitutor() {
    env = System.getProperties().entrySet().stream()
      .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));

    strSubstitutor = new StrSubstitutor(env, "${", "}") {
      @Override
      protected String resolveVariable(final LogEvent event, final String variableName, final StringBuilder buf,
        final int startPos, final int endPos) {
        String result = super.resolveVariable(event, variableName, buf, startPos, endPos);
        if (result == null) {
          throw new TopologyParsingException("Cannot resolve variable: " + variableName);
        }
        return result;
      }

      @Override
      public String replace(final LogEvent event, final String source) {
        if (source == null) {
          return null;
        }
        final StringBuilder buf = new StringBuilder(source);
        try {
          if (!substitute(event, buf, 0, source.length())) {
            return source;
          }
        } catch (Throwable t) {
          StatusLogger.getLogger().error("Replacement failed on {}", source, t);
          throw t;
        }
        return buf.toString();
      }
    };
  }

  public String replace(String original) {
    return strSubstitutor.replace(original);
  }
}
