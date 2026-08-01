package net.livaddons.mixin;

import net.livaddons.feature.FeatureSettings;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces Sodium's selected lava model while preserving lava geometry and flow. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl$DefaultRenderContext")
public abstract class SodiumFluidRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"),
            require = 0)
    private FluidModel livaddons$useWaterModelForLava(FluidStateModelSet models, FluidState state) {
        if (FeatureSettings.lavaToWaterEnabled() && Fluids.LAVA.isSame(state.getType())) {
            return models.get(Fluids.WATER.defaultFluidState());
        }
        return models.get(state);
    }
}
