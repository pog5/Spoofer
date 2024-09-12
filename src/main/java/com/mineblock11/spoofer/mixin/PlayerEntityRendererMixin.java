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

import static com.mineblock11.spoofer.SpooferManager.*;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void getTexture(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfoReturnable<Identifier> cir) {
        var playerName = abstractClientPlayerEntity.getGameProfile().getName();
        if(currentlySpoofed.containsKey(playerName)
        && currentlySpoofed.get(playerName).getRight()) { // Keep Skin Check
            var username = currentlySpoofed.get(abstractClientPlayerEntity.getGameProfile().getName()).getLeft();
            if(TEXTURE_CACHE.containsKey(username)) {
                cir.setReturnValue(TEXTURE_CACHE.get(username));
                return;
            }
            try {
                var id = SkinManager.loadFromFile(SkinManager.downloadSkin(username));
                TEXTURE_CACHE.put(username, id);
                cir.setReturnValue(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Unique
    private AbstractClientPlayerEntity entity;

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"))
    public void renderLabelMixin(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
        entity = abstractClientPlayerEntity;
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    public Text setText(Text text) {
        var playerName = entity.getGameProfile().getName();
        if (!currentlySpoofed.containsKey(playerName))
            return text;

        var newName = currentlySpoofed.get(playerName).getLeft();
        return SpooferManager.replaceStringInTextKeepFormatting(text, playerName, newName);
    }
}
