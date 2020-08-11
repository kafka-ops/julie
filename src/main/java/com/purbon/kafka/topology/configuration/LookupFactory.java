package com.purbon.kafka.topology.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.nerdynick.commons.configuration.VaultConfigUtils;
import com.nerdynick.commons.configuration.interpol.VaultLookup;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.interpol.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LookupFactory.class);
    protected static final Map<String, Function<Configuration, Optional<Lookup>>> knownLookups = new HashMap<>();
    protected static final Function<Configuration, Optional<Lookup>> emptyLookup = c->{
        return Optional.empty();
    };

    static {
        knownLookups.put("vault", c->{
            try {
                return Optional.of(new VaultLookup(VaultConfigUtils.PopulateFromConfigs(c, new VaultConfig(), new SslConfig()).build()));
            } catch (final VaultException e) {
                LOG.error("Failed to create Vault Lookup", e);
            }
            return Optional.empty();
        });
    }

    public Optional<Lookup> get(final String name, final Configuration lookupConfigs){
        return knownLookups.getOrDefault(name.toLowerCase(), emptyLookup).apply(lookupConfigs);
    }
}