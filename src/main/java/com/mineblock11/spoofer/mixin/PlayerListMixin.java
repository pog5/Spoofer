package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SpooferManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListMixin {
    @Final
    @Shadow
    private GameProfile profile;

    @Inject(method = "getDisplayName", at = @At("TAIL"), cancellable = true)
    public void Spoofer$getDisplayName(CallbackInfoReturnable<Text> cir) {
        if (this.profile.getName().equals("CIT-") || !SpooferManager.ENABLE_TAB_SPOOF) {
            return;
        }

        String playerName = this.profile.getName();
        if (SpooferManager.currentlySpoofed.containsKey(playerName)) {
            String spoofedName = SpooferManager.currentlySpoofed.get(playerName).getLeft();
            Text retValue = SpooferManager.replaceStringInTextKeepFormatting(cir.getReturnValue(), playerName, spoofedName);
            cir.setReturnValue(retValue);
        }
    }
}
