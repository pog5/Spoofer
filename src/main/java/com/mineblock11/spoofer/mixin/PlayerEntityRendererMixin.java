package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SkinManager;
import com.mineblock11.spoofer.SpooferManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static com.mineblock11.spoofer.SpooferManager.TEXTURE_CACHE;
import static com.mineblock11.spoofer.SpooferManager.currentlySpoofed;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void getTexture(AbstractClientPlayerEntity playerEntity, CallbackInfoReturnable<Identifier> cir) {
        String playerName = playerEntity.getGameProfile().getName();
        Map.Entry<String, Boolean> spoofEntry = (Map.Entry<String, Boolean>) SpooferManager.currentlySpoofed.get(playerName);

        if (spoofEntry != null && spoofEntry.getValue()) { // Check if spoofed and enabled
            String spoofedUsername = spoofEntry.getKey();

            Identifier cachedTexture = TEXTURE_CACHE.get(spoofedUsername);
            if (cachedTexture != null) {
                cir.setReturnValue(cachedTexture);
                return;
            }

            try {
                Identifier newTexture = SkinManager.loadFromFile(SkinManager.downloadSkin(spoofedUsername));
                TEXTURE_CACHE.put(spoofedUsername, newTexture);
                cir.setReturnValue(newTexture);
            } catch (Exception e) {
                System.err.println("Error loading skin for " + spoofedUsername + ": " + e.getMessage());
                // Handle the exception appropriately, maybe log it and/or use a default skin
            }
        }
    }

    @Unique
    private AbstractClientPlayerEntity currentEntity; // More descriptive name

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"))
    public void storeEntityForLabel(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
        this.currentEntity = abstractClientPlayerEntity;
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    public Text modifyNameLabel(Text text) {
        if (currentEntity == null) {
            return text; // Handle potential null pointer
        }

        String playerName = currentEntity.getGameProfile().getName();
        Map.Entry<String, Boolean> spoofEntry = (Map.Entry<String, Boolean>) SpooferManager.currentlySpoofed.get(playerName);

        if (spoofEntry != null && spoofEntry.getValue()) {
            String newName = spoofEntry.getKey();
            return SpooferManager.replaceStringInTextKeepFormatting(text, playerName, newName);
        }

        return text;
    }
}
