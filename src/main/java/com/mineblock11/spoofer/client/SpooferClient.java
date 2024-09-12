package com.mineblock11.spoofer.client;

import com.mineblock11.spoofer.SpooferManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class SpooferClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            dispatcher.register(literal("spoof")
                    .then(argument("target", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.getOnlinePlayerNames(), builder))
                            .then(argument("username", StringArgumentType.string())
                                    .executes(ctx -> {
                                                var target = StringArgumentType.getString(ctx, "target");
                                                var username = StringArgumentType.getString(ctx, "username");
                                                var keepSkin = false;
                                                if (SpooferManager.currentlySpoofed.containsKey(target)) { // Error if it's already spoofed
                                                    ctx.getSource().sendError(Text.literal("That player is already spoofed! Use /unspoof first."));
                                                    return 1;
                                                }
                                                SpooferManager.currentlySpoofed.put(target, new Pair<>(username, keepSkin));
                                                ctx.getSource().sendFeedback(Text.literal("Spoofed ").append(Text.literal(target).formatted(Formatting.GRAY).append(" as ").append(Text.literal(username).formatted(Formatting.GRAY))));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                    ))).then(argument("keepSkin", BoolArgumentType.bool()).executes(ctx -> {
                        var target = StringArgumentType.getString(ctx, "target");
                        var username = StringArgumentType.getString(ctx, "username");
                        var keepSkin = BoolArgumentType.getBool(ctx, "keepSkin");
                        if (target.equals("ALL")) {
                            for (int i = 0; i < SpooferManager.getOnlinePlayerNames().size() - 1; i++) {
                                SpooferManager.currentlySpoofed.put((String) SpooferManager.getOnlinePlayerNames().toArray()[i], new Pair<>(username + i, keepSkin));
                            }
                            ctx.getSource().sendFeedback(Text.literal("Spoofed all online players"));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (SpooferManager.currentlySpoofed.containsKey(target)) { // Error if it's already spoofed
                            ctx.getSource().sendError(Text.literal("That player is already spoofed! Use /unspoof first."));
                            return 1;
                        }
                        SpooferManager.currentlySpoofed.put(target, new Pair<>(username, keepSkin));
                        ctx.getSource().sendFeedback(Text.literal("Spoofed ").append(Text.literal(target).formatted(Formatting.GRAY)).append(" as ").append(Text.literal(username).formatted(Formatting.GRAY)));
                        return Command.SINGLE_SUCCESS;
                    })));

            dispatcher.register(literal("unspoof")
                    .executes(ctx -> {
                        SpooferManager.currentlySpoofed.clear();
                        ctx.getSource().sendFeedback(Text.literal("Unspoofed everyone").formatted(Formatting.GRAY));
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(argument("target", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.currentlySpoofed.keySet(), builder))
                            .executes(ctx -> {
                                var target = StringArgumentType.getString(ctx, "target");
                                SpooferManager.currentlySpoofed.remove(target);
                                ctx.getSource().sendFeedback(Text.literal("Unspoofed ").append(Text.literal(target).formatted(Formatting.GRAY)));
                                return Command.SINGLE_SUCCESS;
                            }))
            );

            dispatcher.register(literal("togglechatspoof").executes(ctx -> {
                SpooferManager.ENABLE_CHAT_SPOOF = !SpooferManager.ENABLE_CHAT_SPOOF;
                ctx.getSource().sendFeedback(Text.literal("Chat Spoof is now ").append(Text.literal(SpooferManager.ENABLE_CHAT_SPOOF ? "enabled" : "disabled").formatted(SpooferManager.ENABLE_CHAT_SPOOF ? Formatting.GREEN : Formatting.RED).append(".")));
                return Command.SINGLE_SUCCESS;
            }));

            dispatcher.register(literal("spoofnew")
                    .then(argument("username", StringArgumentType.string()))
                    .executes(ctx -> {
                        SpooferManager.SPOOF_NEW_PLAYERS.setLeft(!SpooferManager.SPOOF_NEW_PLAYERS.getLeft());
//                        var spoofName = Text.literal(StringArgumentType.getString(ctx, "username")).formatted(Formatting.GRAY);
                        var stateText = Text.literal(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? "ENABLED" : "DISABLED").formatted(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? Formatting.GREEN : Formatting.RED);
                        ctx.getSource().sendFeedback(Text.literal("Spoofing new joins: ").append(stateText).append(Text.literal(" (not keeping skins)").formatted(Formatting.GRAY)));

                        return Command.SINGLE_SUCCESS;
                    }).then(argument("keepSkin", BoolArgumentType.bool())
                            .executes(ctx -> {
                                SpooferManager.SPOOF_NEW_PLAYERS.setLeft(!SpooferManager.SPOOF_NEW_PLAYERS.getLeft());
//                              var spoofName = Text.literal(StringArgumentType.getString(ctx, "username")).formatted(Formatting.GRAY);
                                var stateText = Text.literal(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? "ENABLED" : "DISABLED").formatted(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? Formatting.GREEN : Formatting.RED);
                                var keepSkin = BoolArgumentType.getBool(ctx, "keepSkin");
                                ctx.getSource().sendFeedback(Text.literal("Spoofing new joins: ").append(stateText).append(Text.literal(" (" + (keepSkin ? " not " : "") + "keeping skins)").formatted(Formatting.GRAY)));

                                return Command.SINGLE_SUCCESS;
                            })));
        }));
    }
}
