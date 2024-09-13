package com.mineblock11.spoofer;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpooferManager implements ModInitializer {

    // TARGET, <SPOOF USERNAME, KEEP SKIN>
    public static final HashMap<String, Pair<String, Boolean>> currentlySpoofed = new HashMap<>();

    public static boolean ENABLE_CHAT_SPOOF = false;
    public static Pair<Boolean, String> SPOOF_NEW_PLAYERS = new Pair<>(false, "");
    public static final HashMap<String, Identifier> TEXTURE_CACHE = new HashMap<>();

    @Override
    public void onInitialize() {}

    public static Text replaceStringInTextKeepFormatting(Text text, String toReplace, String replaceWith) {
        MutableText mutableText = text.copy();
        mutableText.visit((style, source) -> source.equals(toReplace) ?
                        Optional.of(Text.literal(replaceWith).setStyle(style)) :
                        Optional.empty(),
                Style.EMPTY
        );
        return mutableText;
    }

//    public static boolean isValidUsername(String username) {
//        final Pattern usernamePattern = Pattern.compile("^\\w{3,16}$");
//        if (username == null || username.isBlank())
//            return false;
//        return usernamePattern.matcher(username).matches();
//    }

    public static Collection<String> getOnlinePlayerNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            SPOOF_NEW_PLAYERS = new Pair<>(false, "");
            return Collections.emptyList(); // Not in a world yet
        }

        return client.getNetworkHandler().getPlayerList()
                .stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .collect(Collectors.toList());
    }
}
