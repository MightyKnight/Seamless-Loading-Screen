package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.FinishQuit;
import com.minenash.seamless_loading_screen.OnQuitHelper;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @ModifyArg(method = "initWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget;<init>(IIIILnet/minecraft/text/Text;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V", ordinal = 7), index = 5)
    private ButtonWidget.PressAction adjust(ButtonWidget.PressAction onPress){
        return button -> {
            OnQuitHelper.beginScreenshotTask(() -> {
                onPress.onPress(new ButtonWidget(0, 0, 0, 0, Text.of(""), (b) -> {}));
            });
        };
    }

}
