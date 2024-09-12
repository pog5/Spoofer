package com.mineblock11.spoofer.mixin;

import com.mineblock11.spoofer.SkinManager;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.mineblock11.spoofer.SpooferManager.TEXTURE_CACHE;

@Mixin(TitleScreen.class)
public class TitleScreenMenuInitMixin {

    @Unique
    private static boolean hasLoadedTextures = false;

    @Inject(method = "init", at = @At("HEAD"))
    public void titleScreenInit(CallbackInfo ci) {
        if(hasLoadedTextures) return;

        // Load textures once we know resources have fully loaded.
        try {
            File file = new File(String.valueOf(Files.createDirectories(Path.of("skins"))));
            var imgs = file.listFiles();

            assert imgs != null;
            for (File img : imgs) {
                var id = SkinManager.loadFromFile(img);
                System.out.println("Loaded " + id);
                TEXTURE_CACHE.put(img.getName().replace(".png", ""), id);
            }

            hasLoadedTextures = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
