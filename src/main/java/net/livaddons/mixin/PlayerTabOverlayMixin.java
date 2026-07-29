package net.livaddons.mixin;

import net.livaddons.util.ComponentReplacer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            Component replaced = ComponentReplacer.replaceInComponent(original);
            if (replaced != original) {
                cir.setReturnValue(replaced);
            }
        }
    }
}
