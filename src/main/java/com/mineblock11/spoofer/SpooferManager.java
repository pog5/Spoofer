package com.mineblock11.spoofer;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.text.Text.literal;

public class SpooferManager implements ModInitializer {

    // TARGET, <SPOOF USERNAME, KEEP SKIN>
    public static final HashMap<String, Pair<String, Boolean>> currentlySpoofed = new HashMap<>();
    public static final HashMap<String, Identifier> TEXTURE_CACHE = new HashMap<>();
    public static boolean ENABLE_CHAT_SPOOF = false;
    public static Pair<Boolean, String> SPOOF_NEW_PLAYERS = new Pair<>(false, ""); // keepSkin, prefix
    public static Collection<String> PLAYER_LIST = Collections.emptyList();
    public static HashSet<String> AUTOSPOOF_SEEN_PLAYERS = HashSet.newHashSet(20);

    public static Text replaceStringInTextKeepFormatting(Text text, String toReplace, String replaceWith) {
        MutableText newText = literal("");
        text.visit((style, string) -> {
            if (string.contains(toReplace)) {
                String[] split = string.split(toReplace, 2);
                newText.append(literal(split[0]).setStyle(style));
                newText.append(literal(replaceWith).setStyle(style));
                newText.append(literal(split[1]).setStyle(style));
            } else {
                newText.append(literal(string).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return newText;
    }

    public static Collection<String> getOnlinePlayerNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            SPOOF_NEW_PLAYERS = new Pair<>(false, "");
            AUTOSPOOF_SEEN_PLAYERS.clear();
            return Collections.emptyList(); // Not in a world yet
        }
        return client.getNetworkHandler().getPlayerList()
                .stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .filter(name -> !name.contains("CIT-"))
                .collect(Collectors.toList());
    }


//    public static boolean isValidUsername(String username) {
//        final Pattern usernamePattern = Pattern.compile("^\\w{3,16}$");
//        if (username == null || username.isBlank())
//            return false;
//        return usernamePattern.matcher(username).matches();
//    }

    @Override
    public void onInitialize() {
    }
}
