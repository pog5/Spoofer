package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SpooferManager;
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
        if (!SpooferManager.ENABLE_CHAT_SPOOF || text.getString().startsWith("Spoofed"))
            return text;
        for (String target : SpooferManager.currentlySpoofed.keySet()) {
            if(text.toString().contains(target)) {
                var pair = SpooferManager.currentlySpoofed.get(target);
                var newName = pair.getLeft();
                return SpooferManager.replaceStringInTextKeepFormatting(text, target, newName);
            }
        }
        return text;
    }
}