package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.DisplayMode;
import com.minenash.seamless_loading_screen.ServerInfoExtension;
import com.minenash.seamless_loading_screen.config.Config;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerInfo.class)
public class ServerInfoMixin implements ServerInfoExtension {

    @Unique
    private DisplayMode displayMode = Config.defaultServerMode;

    @Inject(method = "toNbt", at = @At("RETURN"))
    private void serialize(CallbackInfoReturnable<NbtCompound> callback) {
        callback.getReturnValue().putString("screenshotDisplayMode", displayMode.toString());
    }

    @Inject(method = "fromNbt", at = @At("RETURN"))
    private static void deserialize(NbtCompound tag, CallbackInfoReturnable<ServerInfo> callback) {
        if (!tag.contains("screenshotDisplayMode", 8)) return;

        ((ServerInfoMixin) (Object) callback.getReturnValue()).displayMode = Enum.valueOf(DisplayMode.class, tag.getString("screenshotDisplayMode"));
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyFrom(ServerInfo info, CallbackInfo callback) {
        displayMode = ((ServerInfoMixin)(Object)info).displayMode;
    }

    @Override
    public void setDisplayMode(DisplayMode b) {
        displayMode = b;
    }

    @Override
    public DisplayMode getDisplayMode() {
        return displayMode;
    }
}
