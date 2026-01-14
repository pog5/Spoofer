package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SkinManager;
import com.mineblock11.spoofer.SpooferManager;
import com.mineblock11.spoofer.config.SpooferConfig;
import com.mineblock11.spoofer.types.ModelSpoofState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
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
public abstract class PlayerEntityRendererMixin<AvatarlikeEntity extends PlayerLikeEntity & ClientPlayerLikeEntity> extends LivingEntityRenderer<AvatarlikeEntity, PlayerEntityRenderState, PlayerEntityModel> {

    @Unique
    private static PlayerEntityRenderState currentEntity; // More descriptive name
    @Unique
    private EntityRendererFactory.Context rendererContext;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("RETURN"))
    private void Spoofer$ctxSetter$init(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.rendererContext = ctx;
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V"))
    public void Spoofer$widthChanger$render(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (playerEntityRenderState.playerName.getString().startsWith("CIT-") || !isValidUsername(playerEntityRenderState.playerName.getString()))
            return;
        ModelSpoofState modelSpoofState = SpooferConfig.getScope().MODEL_SPOOF;

        Pair<String, Boolean> nameAndIsSlim = SpooferManager.getSpoofedNameAndIsSlim(playerEntityRenderState.playerName.getString());
        boolean isSlim = nameAndIsSlim.getRight();
        if (modelSpoofState == ModelSpoofState.STRETCH) {
            if (isSlim) isSlim = false;
        }
        EntityModelLayer normal = EntityModelLayers.PLAYER;
        EntityModelLayer slim = EntityModelLayers.PLAYER_SLIM;
        EntityModelLayer type = isSlim ? slim : normal;
        this.model = new PlayerEntityModel(this.rendererContext.getPart(type), isSlim);
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Lnet/minecraft/util/Identifier;", cancellable = true, at = @At("TAIL"))
    public void Spoofer$skinChanger$getTexture(PlayerEntityRenderState playerEntityRenderState, CallbackInfoReturnable<Identifier> cir) {
        if (SpooferConfig.getScope().MODEL_SPOOF == ModelSpoofState.OFF) return;
        String playerName = playerEntityRenderState.playerName.getString();
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

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    public void Spoofer$storeEntityForLabel(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        currentEntity = playerEntityRenderState;
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", ordinal = 0, at = @At("HEAD"), argsOnly = true)
    public PlayerEntityRenderState Spoofer$modifyNameLabel(PlayerEntityRenderState value) {
        if (currentEntity == null || currentEntity.playerName == null) {
            return value; // Handle potential null pointer
        }

        String playerName = currentEntity.playerName.getString();
        Pair<String, Boolean> spoofEntry = SpooferManager.currentlySpoofed.get(playerName);

        if (spoofEntry != null) {
            String newName = spoofEntry.getLeft();
            value.playerName = SpooferManager.replaceStringInTextKeepFormatting(value.playerName, playerName, newName);
        }

        return value;
    }
}