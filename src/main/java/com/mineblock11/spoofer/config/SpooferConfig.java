package com.mineblock11.spoofer.config;

import com.mineblock11.spoofer.client.SpooferClient;
import com.mineblock11.spoofer.types.SpoofScope;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

public class SpooferConfig {
    public static final String CONFIG_DIR = "config" + File.separator + "spoofer" + File.separator;

    public static HashSet<SpooferOptions> scopes = new HashSet<>();

    public SpooferConfig() {
        scopes.add(getScope(SpoofScope.GLOBAL, Optional.empty()));
    }

    public static SpooferOptions getScope(SpoofScope scope, Optional<String> server) {
        for (SpooferOptions options : scopes) {
            if (options.scope == scope && options.server.equals(server)) {
                return options;
            }
        }
        SpooferOptions newOptions = new SpooferOptions(scope, server);
        scopes.add(newOptions);
        SpooferClient.logger.debug("returned: " + scope.name() + " " + server.orElse(""));
        return newOptions;
    }

    public static SpooferOptions getScope() {
        if (SpooferClient.getCurrentServer() != null) {
            SpooferClient.logger.debug("server scope");
            return getScope(SpoofScope.SERVER, Optional.of(SpooferClient.getCurrentServer()));
        } else {
            SpooferClient.logger.debug("global scope");
            return getScope(SpoofScope.GLOBAL, Optional.empty());
        }
    }

    public static SpooferOptions getDefaultScope() {
        return getScope(SpoofScope.GLOBAL, Optional.empty());
    }
}
