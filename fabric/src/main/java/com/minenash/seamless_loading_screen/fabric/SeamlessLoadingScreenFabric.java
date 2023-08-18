package com.minenash.seamless_loading_screen.fabric;

import com.minenash.seamless_loading_screen.SeamlessLoadingScreen;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SeamlessLoadingScreenFabric implements ClientModInitializer {

    public static final KeyBinding OPEN_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "seamless_loading_screen.keybind.config",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories.misc"));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.wasPressed()) SeamlessLoadingScreen.openSettingsScreen(client);
        });

        SeamlessLoadingScreen.onInitializeClient();

        ShaderEffectManager.getInstance().manageCoreShader(
                new Identifier(SeamlessLoadingScreen.MODID, "blur"),
                VertexFormats.POSITION,
                managedCoreShader -> SeamlessLoadingScreen.BLUR_PROGRAM.load(managedCoreShader.getProgram())
        );
    }
}
