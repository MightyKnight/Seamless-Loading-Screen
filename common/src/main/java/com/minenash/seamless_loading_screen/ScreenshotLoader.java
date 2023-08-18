package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public class ScreenshotLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier SCREENSHOT = new Identifier(SeamlessLoadingScreen.MODID, "screenshot");
    public static double imageRatio = 1;
    public static boolean loaded = false;
    public static DisplayMode displayMode = DisplayMode.ENABLED;

    public static boolean inFade = false;
    public static int time;
    public static float timeDelta;

    private static String fileName = "";

    public static String getFileName() {
        return fileName;
    }

	public static void setScreenshot(String address, int port) {
        setFileName("screenshots/worlds/servers/" + cleanFileName(address) + "_" + port + ".png");
    }

    public static void setScreenshot(String worldName) {
        setFileName("screenshots/worlds/singleplayer/" + worldName + ".png");
    }

    public static void setRealmScreenshot(String realmName) {
        setFileName("screenshots/worlds/realms/" + cleanFileName(realmName) + ".png");
    }

    private static void setFileName(String newFileName){
        fileName = newFileName;
        setScreenshot();
    }

    private static void setScreenshot() {
        loaded = false;

        if (displayMode == DisplayMode.DISABLED)
            return;

        try (InputStream in = new FileInputStream(ScreenshotLoader.fileName)) {
            if(PlatformFunctions.isDevEnv()){
                LOGGER.info("Name: " + ScreenshotLoader.fileName);
            }

            NativeImageBackedTexture image = new NativeImageBackedTexture(NativeImage.read(in));
            MinecraftClient.getInstance().getTextureManager().registerTexture(SCREENSHOT, image);
            imageRatio = image.getImage().getWidth() / (double) image.getImage().getHeight();
            loaded = true;
            time = Config.get().time;
            timeDelta = 1F / Config.get().fade;
            replacebg = true;
        }
        catch (FileNotFoundException ignore){}
        catch (IOException e) {
            LOGGER.error("[SeamlessLoadingScreen]: An Issue has occurred when attempting to set a Screenshot: [name: {}]", ScreenshotLoader.fileName);
            LOGGER.error(String.valueOf(e));
        }
    }

    private static final Pattern RESERVED_FILENAMES_PATTERN = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);
    private static String cleanFileName(String fileName) {
        for (char c : SharedConstants.INVALID_CHARS_LEVEL_NAME) fileName = fileName.replace(c, '_');

        if (RESERVED_FILENAMES_PATTERN.matcher(fileName).matches()) fileName = "_" + fileName + "_";

        if (fileName.length() > 255 - 4) fileName = fileName.substring(0, 255 - 4);

        return fileName;
    }

    public static boolean replacebg = false;

    public static void render(Screen screen, MatrixStack stack) {
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, SCREENSHOT);

        int w = (int) (imageRatio * screen.height);
        DrawableHelper.drawTexture(stack, screen.width / 2 - w / 2, 0, 0.0F, 0.0F, w, screen.height, w, screen.height);
        renderAfterEffects(screen, stack, 1f);
    }

    public static void renderAfterEffects(Screen screen, MatrixStack stack, float fadeValue){
        renderTint(screen, stack, fadeValue);

        if(Config.get().enableScreenshotBlur && SeamlessLoadingScreen.BLUR_PROGRAM.loaded) {
            renderBlur(screen, stack, Config.get().screenshotBlurStrength * fadeValue, Config.get().screenshotBlurQuality);
        }
    }

    public static void renderTint(Screen screen, MatrixStack context, float fadeValue){
        Color color = Config.get().tintColor;

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = Math.round(255 * (Config.get().tintStrength * fadeValue));

        int argb_color = getArgb(alpha, red, green, blue);

        DrawableHelper.fill(stack, 0,0, screen.width, screen.height, argb_color);
    }

    public static int getArgb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    //-----

    public static void renderBlur(Screen screen, MatrixStack stack, float size, float quality){
        var buffer = Tessellator.getInstance().getBuffer();
        var matrix = stack.peek().getPositionMatrix();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(matrix, 0, 0, 0).next();
        buffer.vertex(matrix, 0, screen.height, 0).next();
        buffer.vertex(matrix, screen.width, screen.height, 0).next();
        buffer.vertex(matrix, screen.width, 0, 0).next();

        SeamlessLoadingScreen.BLUR_PROGRAM.setParameters(16, quality, size);
        SeamlessLoadingScreen.BLUR_PROGRAM.use();

        Tessellator.getInstance().draw();
    }

    /**
     * Credit to glisco for <a href="https://github.com/wisp-forest/owo-lib/blob/1.20/src/main/java/io/wispforest/owo/shader/BlurProgram.java">BlurProgram</a>
     * <p>
     * Altered for use with Multi loader
     */
    public static class BlurHelper {
        private GlUniform inputResolution;
        private GlUniform directions;
        private GlUniform quality;
        private GlUniform size;
        private Framebuffer input;

        private ShaderProgram backingProgram;

        public boolean loaded = false;

        public void onWindowResize(MinecraftClient client, Window window){
            if (this.input == null) return;
            this.input.resize(window.getFramebufferWidth(), window.getFramebufferHeight(), MinecraftClient.IS_SYSTEM_MAC);
        }

        public void load(ShaderProgram backingProgram){
            this.backingProgram = backingProgram;
            this.setup();

            this.loaded = true;
        }

        public void setParameters(int directions, float quality, float size) {
            this.directions.set((float) directions);
            this.size.set(size);
            this.quality.set(quality);
        }

        public void use() {
            Framebuffer buffer = MinecraftClient.getInstance().getFramebuffer();

            this.input.beginWrite(false);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo);
            GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight, 0, 0, buffer.textureWidth, buffer.textureHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
            buffer.beginWrite(false);

            this.inputResolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
            this.backingProgram.addSampler("InputSampler", this.input.getColorAttachment());

            RenderSystem.setShader(() -> this.backingProgram);
        }

        protected void setup() {
            this.inputResolution = this.findUniform("InputResolution");
            this.directions = this.findUniform("Directions");
            this.quality = this.findUniform("Quality");
            this.size = this.findUniform("Size");

            Window window = MinecraftClient.getInstance().getWindow();
            this.input = new SimpleFramebuffer(window.getFramebufferWidth(), window.getFramebufferHeight(), false, MinecraftClient.IS_SYSTEM_MAC);
        }

        private GlUniform findUniform(String key){
            return backingProgram.getUniform(key);
        }
    }
}
