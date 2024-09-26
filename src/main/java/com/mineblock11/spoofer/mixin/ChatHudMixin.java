package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SpooferManager;
import com.mineblock11.spoofer.config.SpooferConfig;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public Text modifyAddMessage(Text text) {
        if (!SpooferConfig.getScope().ENABLE_CHAT_SPOOF || text.getString().startsWith("Spoofed")) {
            return text;
        }

        Text newText = text;
        for (var entry : SpooferManager.currentlySpoofed.entrySet()) {
            String target = entry.getKey();
            while (newText.getString().contains(target)) {
                String newName = entry.getValue().getLeft();
                newText = SpooferManager.replaceStringInTextKeepFormatting(newText, target, newName);
            }
        }

        return newText;
    }
}