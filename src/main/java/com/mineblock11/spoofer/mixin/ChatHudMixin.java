package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SpooferManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public Text modifyAddMessage(Text text) {
        if(SpooferManager.ENABLE_CHAT_SPOOF) {
            for (String target : SpooferManager.currentlySpoofed.keySet()) {
                if(text.toString().contains(target)) {
                    var pair = SpooferManager.currentlySpoofed.get(target);
                    var newStr = text.getString().replace(target, pair.getLeft());
                    if(newStr.contains("Spoofed " + pair.getLeft() + " as " + pair.getLeft())) {
                        return text;
                    }
                    return Text.literal(newStr);
                }
            }
        } else {
            return text;
        }
        return text;
    }
}
