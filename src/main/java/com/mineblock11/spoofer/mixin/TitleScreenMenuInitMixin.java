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
        if (hasLoadedTextures) {
            return;
        }

        try {
            Path skinsDirectory = Path.of("skins");
            Files.createDirectories(skinsDirectory); // Create if not exists

            File[] imageFiles = skinsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".png"));

            if (imageFiles != null) {
                for (File img : imageFiles) {
                    try {
                        var id = SkinManager.loadFromFile(img);
                        System.out.println("Loaded " + id);
                        TEXTURE_CACHE.put(img.getName().replace(".png", ""), id);
                    } catch (IOException e) {
                        System.err.println("Error loading skin from file: " + img.getName());
                        e.printStackTrace();
                    }
                }
            }

            hasLoadedTextures = true;
        } catch (IOException e) {
            System.err.println("Error accessing or creating skins directory.");
            e.printStackTrace();
        }
    }
}
