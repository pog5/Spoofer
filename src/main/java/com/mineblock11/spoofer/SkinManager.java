package com.mineblock11.spoofer;

import com.google.gson.JsonArray;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SkinManager {

    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    public static NativeImage toNativeImage(File file){
        try {
            InputStream inputStream = new FileInputStream(file);
            NativeImage nativeImage = NativeImage.read(inputStream);
            inputStream.close();
            return nativeImage;
        }
        catch(Exception e) {
            //happens if there is no file found
            return null;
        }
    }

    private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
        int l;
        int m;
        for(l = x1; l < x2; ++l) {
            for(m = y1; m < y2; ++m) {
                int k = image.getColor(l, m);
                if ((k >> 24 & 255) < 128) {
                    return;
                }
            }
        }

        for(l = x1; l < x2; ++l) {
            for(m = y1; m < y2; ++m) {
                image.setColor(l, m, image.getColor(l, m) & 16777215);
            }
        }

    }

    private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for(int i = x1; i < x2; ++i) {
            for(int j = y1; j < y2; ++j) {
                image.setColor(i, j, image.getColor(i, j) | -16777216);
            }
        }
    }

    public static NativeImage remapTexture(NativeImage nativeImage) {
        int x = nativeImage.getWidth();
        int y = nativeImage.getHeight();
        if (x == 64 && (y == 32 || y == 64)) {
            boolean bl = y == 32;
            if (bl) {
                NativeImage nativeImage2 = new NativeImage(64, 64, true);
                nativeImage2.copyFrom(nativeImage);
                nativeImage.close();
                nativeImage = nativeImage2;
                nativeImage2.fillRect(0, 32, 64, 32, 0);
                nativeImage2.copyRect(4, 16, 16, 32, 4, 4, true, false);
                nativeImage2.copyRect(8, 16, 16, 32, 4, 4, true, false);
                nativeImage2.copyRect(0, 20, 24, 32, 4, 12, true, false);
                nativeImage2.copyRect(4, 20, 16, 32, 4, 12, true, false);
                nativeImage2.copyRect(8, 20, 8, 32, 4, 12, true, false);
                nativeImage2.copyRect(12, 20, 16, 32, 4, 12, true, false);
                nativeImage2.copyRect(44, 16, -8, 32, 4, 4, true, false);
                nativeImage2.copyRect(48, 16, -8, 32, 4, 4, true, false);
                nativeImage2.copyRect(40, 20, 0, 32, 4, 12, true, false);
                nativeImage2.copyRect(44, 20, -8, 32, 4, 12, true, false);
                nativeImage2.copyRect(48, 20, -16, 32, 4, 12, true, false);
                nativeImage2.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            stripAlpha(nativeImage, 0, 0, 32, 16);
            if (bl) {
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

    public static Identifier loadFromFile(File file) {
        Identifier id = Identifier.of("spoofer", file.getName().replace(".png", "").toLowerCase());
        NativeImage rawNativeImage = toNativeImage(file);
        NativeImage processedNativeImage = remapTexture(rawNativeImage);
        NativeImageBackedTexture processedImageBackedTexture = new NativeImageBackedTexture(processedNativeImage);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, processedImageBackedTexture);
        return id;
    }

    public static File downloadSkin(String username) throws Exception {
        //act 1 gets uuid
        String a = getHTML("https://api.mojang.com/users/profiles/minecraft/" + username);
        if(a.isEmpty()) {
            a = getHTML("https://api.mojang.com/users/profiles/minecraft/alex");
        }
        JsonObject json = new JsonParser().parse(a).getAsJsonObject();
        String b = json.get("id").getAsString();

        //for file name
        String usernameR = json.get("name").getAsString();

        //act 2 gets session texture value
        a = getHTML("https://sessionserver.mojang.com/session/minecraft/profile/" + b);

        json = new JsonParser().parse(a).getAsJsonObject();
        JsonArray c = json.getAsJsonArray("properties");
        for(int i = 0; i<c.size(); i++) {
            JsonObject temp = c.get(i).getAsJsonObject();
            b = temp.get("value").getAsString();
        }

        //act 3 decodes texture
        byte[] decoded = Base64.getDecoder().decode(b);
        b = new String(decoded, StandardCharsets.UTF_8);

        //act 4 gets url from texture
        json = new JsonParser().parse(b).getAsJsonObject();
        b = json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        //act 5 downloads image
        URL url = new URL(b);
        BufferedImage img = ImageIO.read(url);
        File file = new File("skins" + File.separator + usernameR + ".png");
        ImageIO.write(img, "png", file);
        return file;
    }
}
