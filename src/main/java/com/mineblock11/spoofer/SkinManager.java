package com.mineblock11.spoofer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SkinManager {

    public static String getHTML(String urlToRead) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // Add User-Agent to avoid 403 errors

        try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    public static NativeImage toNativeImage(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return NativeImage.read(inputStream);
        }
    }

    private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                int color = image.getColor(x, y);
                // Simplified alpha check and color stripping
                if ((color >>> 24) >= 128) {
                    image.setColor(x, y, color & 0xFFFFFF); // Strip alpha
                }
            }
        }
    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                // Simplified alpha setting
                image.setColor(x, y, image.getColor(x, y) | 0xFF000000); // Set alpha to 255
            }
        }
    }

    public static NativeImage remapTexture(NativeImage nativeImage) {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();

        if (width == 64 && (height == 32 || height == 64)) {
            boolean isSlim = height == 32;
            if (isSlim) {
                // Use a temporary NativeImage to avoid closing the original too early
                NativeImage tempImage = new NativeImage(64, 64, true);
                tempImage.copyFrom(nativeImage);
                nativeImage.close();
                nativeImage = tempImage;

                nativeImage.fillRect(0, 32, 64, 32, 0);
                // Simplified copying using loops
                for (int i = 0; i < 2; i++) {
                    nativeImage.copyRect(4 + i * 4, 16, 16, 32, 4, 4, true, false);
                }
                for (int i = 0; i < 4; i++) {
                    nativeImage.copyRect(i * 4, 20, 8, 32, 4, 12, true, false);
                }
                for (int i = 0; i < 2; i++) {
                    nativeImage.copyRect(44 + i * 4, 16, -8, 32, 4, 4, true, false);
                }
                for (int i = 0; i < 4; i++) {
                    nativeImage.copyRect(40 + i * 4, 20, -8 * i, 32, 4, 12, true, false);
                }
            }

            stripAlpha(nativeImage, 0, 0, 32, 16);
            if (isSlim) {
                stripColor(nativeImage, 32, 0, 64, 32);
            }

            stripAlpha(nativeImage, 0, 16, 64, 32);
            stripAlpha(nativeImage, 16, 48, 48, 64);

            return nativeImage;
        } else {
            nativeImage.close();
            return null;
        }
    }

    public static Identifier loadFromFile(File file) throws IOException {
        Identifier id = Identifier.of("spoofer", file.getName().replace(".png", "").toLowerCase());
        NativeImage rawNativeImage = toNativeImage(file);
        NativeImage processedNativeImage = remapTexture(rawNativeImage);
        NativeImageBackedTexture processedImageBackedTexture = new NativeImageBackedTexture(processedNativeImage);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, processedImageBackedTexture);
        return id;
    }

    public static File downloadSkin(String username) throws Exception {
        String uuid = getUUID(username);

        String sessionInfo = getHTML("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        String textureUrl = getTextureURL(sessionInfo);

        URL url = new URL(textureUrl);
        BufferedImage img = ImageIO.read(url);
        File file = new File("skins" + File.separator + username + ".png"); // Use provided username for filename
        ImageIO.write(img, "png", file);
        return file;
    }

    // Helper function to get UUID from Mojang API
    private static String getUUID(String username) throws IOException {
        String response = getHTML("https://api.mojang.com/users/profiles/minecraft/" + username);
        if (response.isEmpty()) {
            response = getHTML("https://api.mojang.com/users/profiles/minecraft/alex");
        }
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.get("id").getAsString();
    }

    // Helper function to get texture URL from session info
    private static String getTextureURL(String sessionInfo) {
        JsonObject json = JsonParser.parseString(sessionInfo).getAsJsonObject();
        JsonArray properties = json.getAsJsonArray("properties");
        for (JsonElement property : properties) {
            JsonObject propertyObj = property.getAsJsonObject();
            if (propertyObj.get("name").getAsString().equals("textures")) {
                String textureData = new String(Base64.getDecoder().decode(propertyObj.get("value").getAsString()), StandardCharsets.UTF_8);
                JsonObject textureJson = JsonParser.parseString(textureData).getAsJsonObject();
                return textureJson.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            }
        }
        return null; // Or throw an exception if texture not found
    }
}
