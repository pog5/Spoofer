package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SkinManager;
import com.mineblock11.spoofer.SpooferManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mineblock11.spoofer.SpooferManager.TEXTURE_CACHE;
import static com.mineblock11.spoofer.SpooferManager.currentlySpoofed;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void getTexture(AbstractClientPlayerEntity playerEntity, CallbackInfoReturnable<Identifier> cir) {
        String playerName = playerEntity.getGameProfile().getName();
        Pair<String, Boolean> spoofEntry = currentlySpoofed.get(playerName);

        if (spoofEntry != null && !spoofEntry.getRight()) { // Check if spoofed and not keeping skin
            // remove 1-3 digit suffix
            String spoofedUsername = spoofEntry.getLeft().replaceFirst("\\d{1,3}$", "");

            Identifier cachedTexture = TEXTURE_CACHE.get(spoofedUsername);
            if (cachedTexture != null) {
                cir.setReturnValue(cachedTexture);
                return;
            }


            Identifier newTexture = null;
            try {
                newTexture = SkinManager.loadFromFile(SkinManager.downloadSkin(spoofedUsername));
                TEXTURE_CACHE.put(spoofedUsername, newTexture);
                cir.setReturnValue(newTexture);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load skin for " + spoofedUsername, e);
            }
        }
    }

    @Unique
    private static AbstractClientPlayerEntity currentEntity; // More descriptive name

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"))
    public void storeEntityForLabel(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
        currentEntity = abstractClientPlayerEntity;
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    public Text modifyNameLabel(Text text) {
        if (currentEntity == null) {
            return text; // Handle potential null pointer
        }

        String playerName = currentEntity.getGameProfile().getName();
        Pair<String, Boolean> spoofEntry = SpooferManager.currentlySpoofed.get(playerName);

        if (spoofEntry != null) {
            String newName = spoofEntry.getLeft();
            return SpooferManager.replaceStringInTextKeepFormatting(text, playerName, newName);
        }

        return text;
    }

    @Inject(method = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("HEAD"))
    private static void Spoofer$modifyIsSkinSlim(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        if (currentEntity == null) {
            return isSlim;
        }

        String playerName = currentEntity.getGameProfile().getName();
        Pair<String, Boolean> spoofEntry = SpooferManager.currentlySpoofed.get(playerName);
        String spoofedUsername = spoofEntry != null ? spoofEntry.getLeft() : null;

        if (spoofedUsername != null) {
            return SkinManager.isSkinSlim(spoofedUsername);
        }

        return spoofEntry != null && !spoofEntry.getRight(); // getRight - KeepSkin
    }
}
