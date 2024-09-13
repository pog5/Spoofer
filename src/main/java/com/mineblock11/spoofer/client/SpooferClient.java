package com.mineblock11.spoofer.client;

import com.mineblock11.spoofer.SpooferManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.Collection;
import java.util.Random;
import java.util.stream.Stream;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class SpooferClient implements ClientModInitializer {
    Random random = new Random();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (SpooferManager.SPOOF_NEW_PLAYERS.getLeft() && !SpooferManager.getOnlinePlayerNames().isEmpty()) {
                SpooferManager.PLAYER_LIST = SpooferManager.getOnlinePlayerNames();
                for (String player : SpooferManager.PLAYER_LIST) {
                    if (SpooferManager.currentlySpoofed.containsKey(player) || SpooferManager.AUTOSPOOF_SEEN_PLAYERS.contains(player))
                        continue;
                    int randomSuffix = random.nextInt(1000);
                    String paddedNumber = String.format("%03d", randomSuffix);
                    SpooferManager.currentlySpoofed.put(player, new Pair<>(SpooferManager.SPOOF_NEW_PLAYERS.getRight() + paddedNumber, SpooferManager.SPOOF_NEW_PLAYERS.getLeft()));
                    SpooferManager.AUTOSPOOF_SEEN_PLAYERS.add(player);
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {

            // /spoof <target> <username> [keepSkin]
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

            // /unspoof [target]
            dispatcher.register(literal("unspoof")
                    .executes(ctx -> {
                        SpooferManager.currentlySpoofed.clear();
                        ctx.getSource().sendFeedback(Text.literal("Unspoofed everyone").formatted(Formatting.GRAY));
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(argument("target", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.currentlySpoofed.keySet(), builder))
                            .executes(ctx -> {
                                String target = StringArgumentType.getString(ctx, "target");
                                SpooferManager.currentlySpoofed.remove(target);
                                ctx.getSource().sendFeedback(Text.literal("Unspoofed ").append(Text.literal(target).formatted(Formatting.GRAY)));
                                return Command.SINGLE_SUCCESS;
                            }))
            );

            // /togglechatspoof
            dispatcher.register(literal("togglechatspoof").executes(ctx -> {
                SpooferManager.ENABLE_CHAT_SPOOF = !SpooferManager.ENABLE_CHAT_SPOOF;
                ctx.getSource().sendFeedback(Text.literal("Chat Spoof is now ")
                        .append(Text.literal(SpooferManager.ENABLE_CHAT_SPOOF ? "enabled" : "disabled")
                                .formatted(SpooferManager.ENABLE_CHAT_SPOOF ? Formatting.GREEN : Formatting.RED))
                        .append("."));
                return Command.SINGLE_SUCCESS;
            }));

            // /spoofnew [namePrefix] [keepSkin]
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

            // /spoofall [namePrefix] [keepSkin]
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
                    ).executes(ctx -> executeSpoofCommand(ctx, "ALL", "fake_name", false)));

            // /whospoof [target]
            dispatcher.register(literal("whospoof")
                    .then(argument("target", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.currentlySpoofed.keySet(), builder))
                            .executes(ctx -> {
                                String target = StringArgumentType.getString(ctx, "target");
                                Pair<String, Boolean> spoofEntry = SpooferManager.currentlySpoofed.get(target);
                                if (spoofEntry == null) {
                                    ctx.getSource().sendFeedback(Text.literal(target + " is not spoofed"));
                                } else {
                                    ctx.getSource().sendFeedback(Text.literal(target + " is spoofed as " + spoofEntry.getLeft()));
                                }
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(Text.literal("Currently spoofed players:"));
                        SpooferManager.currentlySpoofed.forEach((target, spoofEntry) -> {
                            ctx.getSource().sendFeedback(Text.literal(target + " -> " + spoofEntry.getLeft()));
                        });
                        return Command.SINGLE_SUCCESS;
                    }));
        }));
    }

    // Helper function to execute /spoof command logic
    private int executeSpoofCommand(CommandContext<FabricClientCommandSource> ctx, String target, String username, boolean keepSkin) {
        if (target.equals("ALL")) {
            int playerCount = SpooferManager.getOnlinePlayerNames().size();
            for (int i = 0; i < playerCount; i++) {
                SpooferManager.currentlySpoofed.put((String) SpooferManager.getOnlinePlayerNames().toArray()[i], new Pair<>(username + i, keepSkin));
            }
            ctx.getSource().sendFeedback(Text.literal("Spoofed all online players"));
        } else if (SpooferManager.currentlySpoofed.containsKey(target)) {
            ctx.getSource().sendError(Text.literal("That player is already spoofed! Use /unspoof first."));
            return 1; // Indicate failure
        } else {
            SpooferManager.currentlySpoofed.put(target, new Pair<>(username, keepSkin));
            ctx.getSource().sendFeedback(Text.literal("Spoofed ")
                    .append(Text.literal(target).formatted(Formatting.GRAY))
                    .append(" as ")
                    .append(Text.literal(username).formatted(Formatting.GRAY)));
        }
        return Command.SINGLE_SUCCESS;
    }

    // Helper function to execute /spoofnew command logic
    private int executeSpoofNewCommand(CommandContext<FabricClientCommandSource> ctx, boolean keepSkin, String namePrefix) {
        SpooferManager.SPOOF_NEW_PLAYERS.setLeft(!SpooferManager.SPOOF_NEW_PLAYERS.getLeft());
        SpooferManager.SPOOF_NEW_PLAYERS.setRight(namePrefix);
        SpooferManager.AUTOSPOOF_SEEN_PLAYERS.clear();
        String stateText = SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? "ENABLED" : "DISABLED";
        ctx.getSource().sendFeedback(Text.literal("Spoofing new joins as \"" + namePrefix + "\": ")
                .append(Text.literal(stateText).formatted(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? Formatting.GREEN : Formatting.RED))
                .append(Text.literal(" (" + (keepSkin ? "" : "not ") + "keeping skins)").formatted(Formatting.GRAY)));
        return Command.SINGLE_SUCCESS;
    }
}
