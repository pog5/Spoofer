package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SkinManager;
import com.mineblock11.spoofer.SpooferManager;
import com.mineblock11.spoofer.config.SpooferConfig;
import com.mineblock11.spoofer.types.ModelSpoofState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
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

import static com.mineblock11.spoofer.SpooferManager.*;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    @Unique
    private static AbstractClientPlayerEntity currentEntity; // More descriptive name
    private EntityRendererFactory.Context rendererContext;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("RETURN"))
    private void Spoofer$ctxSetter$init(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.rendererContext = ctx;
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    public void Spoofer$widthChanger$render(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (player.getGameProfile().getName().startsWith("CIT-") || !isValidUsername(player.getGameProfile().getName()))
            return;
        ModelSpoofState modelSpoofState = SpooferConfig.getScope().MODEL_SPOOF;

        Pair<String, Boolean> nameAndIsSlim = SpooferManager.getSpoofedNameAndIsSlim(player.getGameProfile().getName());
        boolean isSlim = nameAndIsSlim.getRight();
        if (modelSpoofState == ModelSpoofState.STRETCH) {
            if (isSlim) isSlim = false;
        }
        this.model = new PlayerEntityModel<>(this.rendererContext.getPart(isSlim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), isSlim);
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void Spoofer$skinChanger$getTexture(AbstractClientPlayerEntity playerEntity, CallbackInfoReturnable<Identifier> cir) {
        if (SpooferConfig.getScope().MODEL_SPOOF == ModelSpoofState.OFF) return;
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

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"))
    public void Spoofer$storeEntityForLabel(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, float f, CallbackInfo ci) {
        currentEntity = abstractClientPlayerEntity;
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    public Text Spoofer$modifyNameLabel(Text text) {
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
}