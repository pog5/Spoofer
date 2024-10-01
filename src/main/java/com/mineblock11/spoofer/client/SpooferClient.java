package com.mineblock11.spoofer.client;

import com.mineblock11.spoofer.SpooferManager;
import com.mineblock11.spoofer.config.SpooferConfig;
import com.mineblock11.spoofer.types.ModelSpoofState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class SpooferClient implements ClientModInitializer {
    private static int randomNameSuffix = 0;
    public static Logger logger = LoggerFactory.getLogger("spoofer");

    @Override
    public void onInitializeClient() {
        logger.info("Spoofer Entry");
        SpooferConfig.getScope().save();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MinecraftClient.getInstance().player != null &&
                    SpooferConfig.getScope().SPOOF_NEW_PLAYERS_PREFIX != null && !(Objects.equals(SpooferConfig.getScope().SPOOF_NEW_PLAYERS_PREFIX, "<disabled>")) &&
                    !SpooferManager.getOnlinePlayerNames().isEmpty()) {
                handleSpoofNewPlayers();
            }
        });

        Pair<Boolean, Boolean> pair = new Pair<>(false, false);

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            registerSpoofCommand(dispatcher);
            registerUnspoofCommand(dispatcher);
            registerToggleChatSpoofCommand(dispatcher);
            registerToggleTabSpoofCommand(dispatcher);
            registerToggleSpoofFeedbackCommand(dispatcher);
            registerSpoofNewCommand(dispatcher);
            registerSpoofAllCommand(dispatcher);
            registerWhoSpoofCommand(dispatcher);
            registerSpoofNextCommand(dispatcher);
            registerSpoofNextClearCommand(dispatcher);
            registerToggleModelSpoofCommand(dispatcher);
        }));

        logger.info("Spoofer Entry done!");
    }

    private void handleSpoofNewPlayers() {
        if (!SpooferManager.PLAYER_LIST.equals(SpooferManager.getOnlinePlayerNames()) && !SpooferManager.SPOOF_NEXT_JOIN.isEmpty()) {
            handleSpoofNextJoin();
        }

        SpooferManager.PLAYER_LIST = SpooferManager.getOnlinePlayerNames();
        for (String player : SpooferManager.PLAYER_LIST) {
            if (SpooferManager.currentlySpoofed.containsKey(player) || SpooferManager.AUTOSPOOF_SEEN_PLAYERS.contains(player)) {
                continue;
            }
            spoofPlayer(player, SpooferConfig.getScope().SPOOF_NEW_PLAYERS_PREFIX, SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN);
            SpooferManager.AUTOSPOOF_SEEN_PLAYERS.add(player);
        }
    }

    private void handleSpoofNextJoin() {
        Pair<String, Boolean> entry = SpooferManager.SPOOF_NEXT_JOIN.stream().findFirst().get();
        String username = entry.getLeft();
        boolean keepSkin = entry.getRight();
        SpooferManager.SPOOF_NEXT_JOIN.remove(entry);
        spoofPlayer(username, username, keepSkin);
    }

    private void spoofPlayer(String realName, String namePrefix, boolean keepSkin) {
        int randomSuffix = randomNameSuffix++;
        String paddedNumber = String.format("%03d", randomSuffix);
        SpooferManager.currentlySpoofed.put(realName, new Pair<>(namePrefix + paddedNumber, keepSkin));
    }

    private void registerSpoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spoof")
                .then(argument("target", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(() -> SpooferManager.getOnlinePlayerNames().stream().iterator(), builder))
                        .then(argument("username", StringArgumentType.string())
                                .then(argument("keepSkin", BoolArgumentType.bool())
                                        .executes(ctx -> executeSpoofCommand(ctx,
                                                StringArgumentType.getString(ctx, "target"),
                                                StringArgumentType.getString(ctx, "username"),
                                                BoolArgumentType.getBool(ctx, "keepSkin"))
                                        ))
                                .executes(ctx -> executeSpoofCommand(ctx,
                                        StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "username"),
                                        false))
                        )));
    }

    private void registerUnspoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("unspoof")
                .executes(ctx -> {
                    SpooferManager.currentlySpoofed.clear();
                    sendFeedback(ctx, "Unspoofed everyone", Formatting.GRAY);
                SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN = false;
                    randomNameSuffix = 0;
                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("target", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.currentlySpoofed.keySet(), builder))
                        .executes(ctx -> {
                            String target = StringArgumentType.getString(ctx, "target");
                            SpooferManager.currentlySpoofed.remove(target);
                            sendFeedback(ctx, "Unspoofed " + target, Formatting.GRAY);
                            return Command.SINGLE_SUCCESS;
                        }))
        );
    }

    private void registerToggleChatSpoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("togglechatspoof").executes(ctx -> {
            SpooferConfig.getScope().ENABLE_CHAT_SPOOF = !SpooferConfig.getScope().ENABLE_CHAT_SPOOF;
            sendFeedback(ctx, "Chat Spoof is now " + (SpooferConfig.getScope().ENABLE_CHAT_SPOOF ? "enabled" : "disabled"),
                    SpooferConfig.getScope().ENABLE_CHAT_SPOOF ? Formatting.GREEN : Formatting.RED);
            return Command.SINGLE_SUCCESS;
        }));
    }

    private void registerToggleTabSpoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("toggletabspoof").executes(ctx -> {
            SpooferConfig.getScope().ENABLE_TAB_SPOOF = !SpooferConfig.getScope().ENABLE_TAB_SPOOF;
            sendFeedback(ctx, "TAB Spoof is now " + (SpooferConfig.getScope().ENABLE_TAB_SPOOF ? "enabled" : "disabled"),
                    SpooferConfig.getScope().ENABLE_TAB_SPOOF ? Formatting.GREEN : Formatting.RED);
            return Command.SINGLE_SUCCESS;
        }));
    }

    private void registerToggleSpoofFeedbackCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("togglespooffeedback").executes(ctx -> {
            SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK = !SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK;
            sendFeedback(ctx, "Spoof feedback is now " + (SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK ? "enabled" : "disabled"),
                    SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK ? Formatting.GREEN : Formatting.RED);
            return Command.SINGLE_SUCCESS;
        }));
    }

    private void registerToggleModelSpoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("togglemodelspoof").then(argument("status", StringArgumentType.string())
                .suggests((ctx, builder) -> CommandSource.suggestMatching(() -> Stream.of("off", "stretch", "model").iterator(), builder))
                .executes(ctx -> {
                    String stateString = StringArgumentType.getString(ctx, "status");
                    ModelSpoofState state = switch (stateString) {
                        case "off" -> ModelSpoofState.OFF;
                        case "stretch" -> ModelSpoofState.STRETCH;
                        case "model" -> ModelSpoofState.MODEL;
                        default -> throw new SimpleCommandExceptionType(new LiteralMessage("Invalid model spoof state")).create();
                    };

                    SpooferConfig.getScope().MODEL_SPOOF = state;
                    sendFeedback(ctx, "Model Spoof is now set to " + state.name().toLowerCase(),
                            state == ModelSpoofState.OFF ? Formatting.RED : Formatting.GREEN);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private void registerSpoofNewCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spoofnew")
                .then(argument("namePrefix", StringArgumentType.string())
                        .then(argument("keepSkin", BoolArgumentType.bool())
                                .executes(ctx -> executeSpoofNewCommand(ctx,
                                        BoolArgumentType.getBool(ctx, "keepSkin"),
                                        StringArgumentType.getString(ctx, "namePrefix"))
                                ))
                        .executes(ctx -> executeSpoofNewCommand(ctx, false, StringArgumentType.getString(ctx, "namePrefix"))
                        )
                ).executes(ctx -> executeSpoofNewCommand(ctx, false, "fake_name")));
    }

    private void registerSpoofAllCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spoofall")
                .then(argument("namePrefix", StringArgumentType.string())
                        .then(argument("keepSkin", BoolArgumentType.bool())
                                .executes(ctx -> executeSpoofCommand(ctx,
                                        "ALL",
                                        StringArgumentType.getString(ctx, "namePrefix"),
                                        BoolArgumentType.getBool(ctx, "keepSkin"))
                                ))
                        .executes(ctx -> executeSpoofCommand(ctx, "ALL", StringArgumentType.getString(ctx, "namePrefix"), false)
                        )
                ).executes(ctx -> {
                    SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN = true;
                    return executeSpoofCommand(ctx, "ALL", "fake_name", false);
                }));
    }

    private void registerWhoSpoofCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("whospoof")
                .then(argument("target", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(() -> {
                            Collection<String> spoofedRealNames = SpooferManager.currentlySpoofed.keySet().stream().toList();
                            Collection<String> spoofedNames = SpooferManager.currentlySpoofed.values().stream()
                                    .map(Pair::getLeft)
                                    .toList();
                            return Stream.concat(spoofedNames.stream(), spoofedRealNames.stream()).iterator();
                        }, builder))
                        .executes(ctx -> executeWhoSpoofCommand(ctx, StringArgumentType.getString(ctx, "target")))
                )
                .executes(ctx -> {
                    boolean wasEnabled = SpooferConfig.getScope().ENABLE_CHAT_SPOOF;
                    if (wasEnabled) {
                        SpooferConfig.getScope().ENABLE_CHAT_SPOOF = false;
                    }

                    sendFeedback(ctx, "Currently spoofed players:");
                    SpooferManager.currentlySpoofed.forEach((realName, spoofEntry) -> {
                        sendFeedback(ctx, realName + " -> " + spoofEntry.getLeft());
                    });

                    if (wasEnabled) {
                        SpooferConfig.getScope().ENABLE_CHAT_SPOOF = true;
                    }
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private void registerSpoofNextCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spoofnext")
                .then(argument("username", StringArgumentType.string())
                        .then(argument("keepSkin", BoolArgumentType.bool())
                                .executes(ctx -> executeSpoofNextCommand(ctx,
                                        StringArgumentType.getString(ctx, "username"),
                                        BoolArgumentType.getBool(ctx, "keepSkin"))
                                ))
                        .executes(ctx -> executeSpoofNextCommand(ctx,
                                StringArgumentType.getString(ctx, "username"),
                                false)
                        )
                ));
    }

    private void registerSpoofNextClearCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spoofnextclear").executes(ctx -> {
            SpooferManager.SPOOF_NEXT_JOIN.clear();
            sendFeedback(ctx, "Cleared next join spoofs");
            return Command.SINGLE_SUCCESS;
        }));
    }

    private void registerSpooferDebugCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("spooferdebug").executes(ctx -> {
            sendFeedback(ctx, "Current server: " + getCurrentServer());
            return Command.SINGLE_SUCCESS;
        }).then(literal("clearcache").executes(ctx -> {
            clearCache(ctx);
            return Command.SINGLE_SUCCESS;
        })));
    }

    private int executeSpoofCommand(CommandContext<FabricClientCommandSource> ctx, String target, String username, boolean keepSkin) {
        if (target.equals("ALL")) {
            int playerCount = SpooferManager.getOnlinePlayerNames().size();
            for (int i = 0; i < playerCount; i++) {
                SpooferManager.currentlySpoofed.put((String) SpooferManager.getOnlinePlayerNames().toArray()[i], new Pair<>(username + i, keepSkin));
            }
            sendFeedback(ctx, "Spoofed all online players");
        } else if (SpooferManager.currentlySpoofed.containsKey(target)) {
            sendError(ctx, "That player is already spoofed! Use /unspoof first.");
            return 1; // Indicate failure
        } else {
            SpooferManager.currentlySpoofed.put(target, new Pair<>(username, keepSkin));
            sendFeedback(ctx, "Spoofed " + target + " as " + username);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeSpoofNewCommand(CommandContext<FabricClientCommandSource> ctx, boolean keepSkin, String namePrefix) {
        SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN = !SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN;
        SpooferConfig.getScope().SPOOF_NEW_PLAYERS_PREFIX = namePrefix;
        SpooferManager.AUTOSPOOF_SEEN_PLAYERS.clear();
        String stateText = SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN ? "ENABLED" : "DISABLED";
        sendFeedback(ctx, "Spoofing new joins as \"" + namePrefix + "\": " + stateText + " (" + (keepSkin ? "" : "not ") + "keeping skins)",
                SpooferConfig.getScope().SPOOF_NEW_PLAYERS_KEEPSKIN ? Formatting.GREEN : Formatting.RED);
        return Command.SINGLE_SUCCESS;
    }

    private int executeSpoofNextCommand(CommandContext<FabricClientCommandSource> ctx, String username, boolean keepSkin) {
        var spoofNextMap = SpooferManager.SPOOF_NEXT_JOIN;
        spoofNextMap.add(new Pair<>(username, keepSkin));
        sendFeedback(ctx, "Next player to join will be spoofed as " + username + " (" + (keepSkin ? "" : "not ") + "keeping skin)");
        return Command.SINGLE_SUCCESS;
    }

    private int executeWhoSpoofCommand(CommandContext<FabricClientCommandSource> ctx, String target) {
        boolean wasEnabled = SpooferConfig.getScope().ENABLE_CHAT_SPOOF;
        if (wasEnabled) {
            SpooferConfig.getScope().ENABLE_CHAT_SPOOF = false;
        }

        String spoofedName = SpooferManager.getSpoofedName(target);
        if (spoofedName == null) {
            handleWhoSpoofOriginalName(ctx, target);
        } else {
            sendFeedback(ctx, target + " is spoofed as " + spoofedName);
        }

        if (wasEnabled) {
            SpooferConfig.getScope().ENABLE_CHAT_SPOOF = true;
        }
        return Command.SINGLE_SUCCESS;
    }

    private void handleWhoSpoofOriginalName(CommandContext<FabricClientCommandSource> ctx, String target) {
        String ogName = SpooferManager.getOriginalName(target);
        if (ogName != null) {
            if (SpooferManager.currentlySpoofed.get(ogName) == null) {
                sendError(ctx, ogName + " is not spoofed/not in your server");
            } else {
                sendFeedback(ctx, ogName + " is spoofed as " + SpooferManager.currentlySpoofed.get(ogName).getLeft());
            }
        } else {
            sendError(ctx, target + " is not spoofed");
        }
    }

    private void sendFeedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        if (SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK) {
            ctx.getSource().sendFeedback(Text.literal(message));
        }
    }

    private void sendFeedback(CommandContext<FabricClientCommandSource> ctx, String message, Formatting formatting) {
        if (SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK) {
            ctx.getSource().sendFeedback(Text.literal(message).formatted(formatting));
        }
    }

    private void sendError(CommandContext<FabricClientCommandSource> ctx, String message) {
        if (SpooferConfig.getScope().ENABLE_SPOOF_FEEDBACK) {
            ctx.getSource().sendError(Text.literal(message));
        }
    }

    private void clearCache(CommandContext<FabricClientCommandSource> ctx) {
        SpooferManager.TEXTURE_CACHE.clear();
        sendFeedback(ctx, "Cleared skin cache");
    }

    @Nullable
    public static String getCurrentServer() {
        try {
            return Objects.requireNonNull(Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getServerInfo()).address;
        } catch (NullPointerException e){
            return null;
        }
    }
}