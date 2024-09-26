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
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SkinManager {

    public static String getHTML(String urlToRead) throws IOException, URISyntaxException {
        StringBuilder result = new StringBuilder();
        URI uri = new URI(urlToRead);
        URL url = uri.toURL();
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
        String textureUrl;
        try {
            String uuid = getUUID(username);

            String sessionInfo = getHTML("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            textureUrl = getTextureURL(sessionInfo);
        } catch (Exception e) {
            System.err.println("[SPOOFER]: Error loading skin for " + e.getMessage());
            System.err.println("[SPOOFER]: Falling back to Steve skin");
            String uuid = getUUID("Steve");
            String sessionInfo = getHTML("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            textureUrl = getTextureURL(sessionInfo);
        }

        assert textureUrl != null;
        URI uri = new URI(textureUrl);
        URL url = uri.toURL();
        BufferedImage img = ImageIO.read(url);
        File file = new File("skins" + File.separator + username + ".png"); // Use provided username for filename
        ImageIO.write(img, "png", file);
        return file;
    }

    // Helper function to get UUID from Mojang API
    private static String getUUID(String username) throws IOException, URISyntaxException {
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

    public static boolean isSkinSlim(String username) {
        if (SpooferManager.SLIM_CACHE.containsKey(username))
            return SpooferManager.SLIM_CACHE.get(username);
        if (username == null || username.isBlank() || !SpooferManager.isValidUsername(username)) {
            return false;
        }
        try {
            String uuid = getUUID(username);
            String sessionInfo = getHTML("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            String textureUrl = getTextureURL(sessionInfo);
            URI uri = new URI(textureUrl);
            URL url = uri.toURL();
            BufferedImage img = ImageIO.read(url);
//            return img.getHeight() == 32;
            boolean status =  isSlimSkin(img);
            SpooferManager.SLIM_CACHE.put(username, status);
            return status;
        } catch (Exception e) {
            System.err.println("[SPOOFER]: Error loading skin for " + username);
            SpooferManager.SLIM_CACHE.put(username, false);
            return false;
        }
    }

    public static boolean isSkinSlim(URI url) {
        try {
            BufferedImage img = ImageIO.read(url.toURL());
            return isSlimSkin(img);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isSlimSkin(BufferedImage skin) {
        int width = skin.getWidth();
        int height = skin.getHeight();

        // Verify valid skin dimensions (64x64 or 64x32)
        if (!((width == 64 && height == 64) || (width == 64 && height == 32))) {
            System.out.println("Invalid skin dimensions.");
            return false; // Default to classic model if dimensions are invalid
        }

        // Coordinates to check for transparency on the outermost arm pixels
        int[][] coordinatesToCheck = {
                // Right Arm Main Layer (outermost column)
                {44, 16}, {44, 17}, {44, 18}, {44, 19}, {44, 20}, {44, 21}, {44, 22}, {44, 23},
                {44, 24}, {44, 25}, {44, 26}, {44, 27}, {44, 28}, {44, 29}, {44, 30}, {44, 31},
                // Left Arm Main Layer (outermost column)
                {36, 48}, {36, 49}, {36, 50}, {36, 51}, {36, 52}, {36, 53}, {36, 54}, {36, 55},
                {36, 56}, {36, 57}, {36, 58}, {36, 59}, {36, 60}, {36, 61}, {36, 62}, {36, 63}
                // Similarly, you can add coordinates for the second layers if needed
        };

        int transparentPixelCount = 0;
        int totalPixels = coordinatesToCheck.length;

        for (int[] coord : coordinatesToCheck) {
            int x = coord[0];
            int y = coord[1];
            if (x >= width || y >= height) {
                continue; // Skip if out of bounds
            }
            int pixel = skin.getRGB(x, y);
            int alpha = (pixel >> 24) & 0xff;
            if (alpha == 0) {
                transparentPixelCount++;
            }
        }

        // Determine if the majority of the outer arm pixels are transparent
        double transparentRatio = (double) transparentPixelCount / totalPixels;
        return transparentRatio > 0.9; // Threshold can be adjusted based on requirements
    }
}
