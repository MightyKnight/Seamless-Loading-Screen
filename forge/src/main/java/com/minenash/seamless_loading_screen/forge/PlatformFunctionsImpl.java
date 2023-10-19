package com.minenash.seamless_loading_screen.forge;

import com.minenash.seamless_loading_screen.PlatformFunctions;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class PlatformFunctionsImpl {

    /**
     * This is our actual method to {@link PlatformFunctions#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isClientEnv() {
        return FMLLoader.getDist().isClient();
    }

    //--

    public static Path getGameDir() {
        return FMLLoader.getGamePath();
    }

    public static boolean isDevEnv() {
        return !FMLLoader.isProduction();
    }
}
