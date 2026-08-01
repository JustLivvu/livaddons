package net.livaddons.mixin;

import net.livaddons.feature.LavaToWater;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Redirect(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"))
    private FluidModel livaddons$useWaterModelForLava(FluidStateModelSet models, FluidState state) {
        if (LavaToWater.active() && Fluids.LAVA.isSame(state.getType())) {
            return models.get(Fluids.WATER.defaultFluidState());
        }
        return models.get(state);
    }
}
