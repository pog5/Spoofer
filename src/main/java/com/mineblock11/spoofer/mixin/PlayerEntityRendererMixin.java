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

import java.net.URI;
import java.net.URISyntaxException;

import static com.mineblock11.spoofer.SpooferManager.TEXTURE_CACHE;
import static com.mineblock11.spoofer.SpooferManager.currentlySpoofed;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    @Unique
    private static AbstractClientPlayerEntity currentEntity; // More descriptive name
    private EntityRendererFactory.Context rendererContext;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx,
                                     PlayerEntityModel<AbstractClientPlayerEntity> model,
                                     float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("RETURN"))
    private void onPlayerEntityRendererInit(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.rendererContext = ctx;
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    public void render(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci)  {
        String realName = player.getGameProfile().getName();
        boolean shouldSpoofThisPlayer = SpooferManager.currentlySpoofed.containsKey(realName);
        boolean keepSkin = true;
        if (SpooferManager.currentlySpoofed.containsKey(realName))
            keepSkin = SpooferManager.currentlySpoofed.get(realName).getRight();
        boolean isPlayerNpc = realName.startsWith("CIT-");

        if (isPlayerNpc || SpooferConfig.getScope().MODEL_SPOOF == ModelSpoofState.OFF) {
            return;
        }

        if (shouldSpoofThisPlayer) {
            boolean isSpoofedSkinSlim;
            if (SpooferConfig.getScope().MODEL_SPOOF == ModelSpoofState.STRETCH) {
                isSpoofedSkinSlim = false;
            } else {
                Pair<String, Boolean> spoofEntry = currentlySpoofed.get(realName);
                String spoofedName = spoofEntry != null ? spoofEntry.getLeft() : null;
                if (spoofedName == null) {
                    return;
                }
                isSpoofedSkinSlim = SkinManager.isSkinSlim(spoofedName);
            }

            // Create and set the appropriate model
            this.model = new PlayerEntityModel<>(this.rendererContext.getPart(isSpoofedSkinSlim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), isSpoofedSkinSlim);
        } else {
            boolean isSlim = SkinManager.isSkinSlim(player.getName().getString());

            this.model = new PlayerEntityModel<>(this.rendererContext.getPart(isSlim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), isSlim);
        }
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void Spoofer$getTexture(AbstractClientPlayerEntity playerEntity, CallbackInfoReturnable<Identifier> cir) {
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
//
//    @ModifyVariable(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/model/ModelPart;Z)Lnet/minecraft/client/render/entity/model/PlayerEntityModel;"), argsOnly = true)
//    private static boolean Spoofer$modifySlimModel(boolean isSlim) {
//        if (currentEntity == null) {
//            return isSlim;
//        }
//
//        String playerName = currentEntity.getGameProfile().getName();
//        Pair<String, Boolean> spoofEntry = SpooferManager.currentlySpoofed.get(playerName);
//        String spoofedName = spoofEntry != null ? spoofEntry.getLeft() : null;
//        if (spoofedName == null) {
//            return isSlim;
//        }
//
//        return SkinManager.isSkinSlim(spoofedName);
//    }


}