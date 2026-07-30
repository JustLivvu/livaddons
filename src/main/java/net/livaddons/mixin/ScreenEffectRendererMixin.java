package net.livaddons.mixin;

import net.livaddons.feature.FeatureSettings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Redirect(
            method = "renderScreenEffect",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z")
    )
    private boolean livaddons$hideFireOverlay(LocalPlayer player) {
        return !FeatureSettings.disableFireEnabled() && player.isOnFire();
    }
}
