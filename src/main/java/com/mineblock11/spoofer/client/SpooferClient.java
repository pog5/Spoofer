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

            // /spoof <target> <username> [keepSkin]
            dispatcher.register(literal("spoof")
                    .then(argument("target", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(SpooferManager.getOnlinePlayerNames(), builder))
                            .then(argument("username", StringArgumentType.string())
                                    .then(argument("keepSkin", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                return executeSpoofCommand(ctx,
                                                        StringArgumentType.getString(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "username"),
                                                        BoolArgumentType.getBool(ctx, "keepSkin"));
                                            }))
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

            // /spoofnew [keepSkin]
            dispatcher.register(literal("spoofnew")
                    .then(argument("keepSkin", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean keepSkin = BoolArgumentType.getBool(ctx, "keepSkin");
                                return executeSpoofNewCommand(ctx, keepSkin);
                            }))
                    .executes(ctx -> executeSpoofNewCommand(ctx, false)));
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
    private int executeSpoofNewCommand(CommandContext<FabricClientCommandSource> ctx, boolean keepSkin) {
        SpooferManager.SPOOF_NEW_PLAYERS.setLeft(!SpooferManager.SPOOF_NEW_PLAYERS.getLeft());
        String stateText = SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? "ENABLED" : "DISABLED";
        ctx.getSource().sendFeedback(Text.literal("Spoofing new joins: ")
                .append(Text.literal(stateText).formatted(SpooferManager.SPOOF_NEW_PLAYERS.getLeft() ? Formatting.GREEN : Formatting.RED))
                .append(Text.literal(" (" + (keepSkin ? "" : "not ") + "keeping skins)").formatted(Formatting.GRAY)));
        return Command.SINGLE_SUCCESS;
    }
}
