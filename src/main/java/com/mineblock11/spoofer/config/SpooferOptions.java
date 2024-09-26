package com.mineblock11.spoofer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mineblock11.spoofer.client.SpooferClient;
import com.mineblock11.spoofer.types.ModelSpoofState;
import com.mineblock11.spoofer.types.SpoofScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class SpooferOptions {
    public SpoofScope scope;
    public Optional<String> server;

    @Expose
    public boolean ENABLE_SPOOF_FEEDBACK;
    @Expose
    public boolean ENABLE_CHAT_SPOOF;
    @Expose
    public boolean ENABLE_TAB_SPOOF;
    @Expose
    public ModelSpoofState MODEL_SPOOF;
    @Expose
    public String SPOOF_NEW_PLAYERS_PREFIX;
    @Expose
    public boolean SPOOF_NEW_PLAYERS_KEEPSKIN;

    public SpooferOptions(SpoofScope scope, Optional<String> server) {
        this.scope = scope;
        this.server = server;
        if (scope == SpoofScope.SERVER && server.isEmpty()) {
            throw new IllegalArgumentException("Server must be present when scope is SERVER");
        }

        // Load configuration for the current scope
        File configFile = new File(SpooferConfig.CONFIG_DIR + scope.name().toLowerCase() + File.separator + server.orElse("global") + ".json");

        if (configFile.isFile()) {
            SpooferClient.logger.info("file for " + scope.name() + " " + server.orElse("") + " exists, loading");
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            SpooferOptions loadedOptions = null;
            try {
                loadedOptions = gson.fromJson(new String(Files.readAllBytes(configFile.toPath())), SpooferOptions.class);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Assign loaded values to the current object
            assert loadedOptions != null;
            ENABLE_SPOOF_FEEDBACK = loadedOptions.ENABLE_SPOOF_FEEDBACK;
            ENABLE_CHAT_SPOOF = loadedOptions.ENABLE_CHAT_SPOOF;
            ENABLE_TAB_SPOOF = loadedOptions.ENABLE_TAB_SPOOF;
            MODEL_SPOOF = loadedOptions.MODEL_SPOOF;
            SPOOF_NEW_PLAYERS_PREFIX = loadedOptions.SPOOF_NEW_PLAYERS_PREFIX;
            SPOOF_NEW_PLAYERS_KEEPSKIN = loadedOptions.SPOOF_NEW_PLAYERS_KEEPSKIN;
        } else {
            if (scope == SpoofScope.GLOBAL) {
                ENABLE_SPOOF_FEEDBACK = true;
                ENABLE_CHAT_SPOOF = true;
                ENABLE_TAB_SPOOF = true;
                MODEL_SPOOF = ModelSpoofState.OFF;
                SPOOF_NEW_PLAYERS_PREFIX = "<disabled>";
                SPOOF_NEW_PLAYERS_KEEPSKIN = true;
            } else {
                final Optional<SpooferOptions> globalOptions = SpooferConfig.scopes.stream().filter(opt -> opt.scope == SpoofScope.GLOBAL).findFirst();
                if (globalOptions.isEmpty()) {
                    throw new IllegalStateException("Global options not found");
                }

                ENABLE_SPOOF_FEEDBACK = globalOptions.get().ENABLE_SPOOF_FEEDBACK;
                ENABLE_CHAT_SPOOF = globalOptions.get().ENABLE_CHAT_SPOOF;
                ENABLE_TAB_SPOOF = globalOptions.get().ENABLE_TAB_SPOOF;
                MODEL_SPOOF = globalOptions.get().MODEL_SPOOF;
                SPOOF_NEW_PLAYERS_PREFIX = globalOptions.get().SPOOF_NEW_PLAYERS_PREFIX;
            }
        }
        save();
    }

    public void save() {
        File file = new File(SpooferConfig.CONFIG_DIR + scope.name().toLowerCase() + File.separator + server.orElse("global") + ".json");
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String serialized = gson.toJson(this);

        boolean delResult = file.delete();
        if (delResult) {
            SpooferClient.logger.debug("old config file doesn't exist or couldn't delete");
        }

        try {
            Files.write(file.toPath(), serialized.getBytes());
            SpooferClient.logger.debug("Saved");
        } catch (IOException e) {
            SpooferClient.logger.debug("making dirs");
            try {
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), serialized.getBytes());
                SpooferClient.logger.debug("Saved");
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }
    }

}
