package com.mineblock11.spoofer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mineblock11.spoofer.config.SpooferConfig;
import com.mineblock11.spoofer.types.ModelSpoofState;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static com.mineblock11.spoofer.SpooferManager.isValidUsername;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/AssetInfo$TextureAsset;texturePath()Lnet/minecraft/util/Identifier;"))
    public Identifier Spoofer$capeTextureChanger$render(AssetInfo.TextureAsset instance, Operation<Identifier> original) {
//        String name = instance.getGameProfile().getName();
//        if (name.startsWith("CIT-") || !isValidUsername(name) || SpooferConfig.getScope().MODEL_SPOOF == ModelSpoofState.OFF) return original.call(instance);

        // TODO: Implement cape texture spoofing
        return original.call(instance);
    }
}
