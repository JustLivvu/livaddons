package net.livaddons.mixin;

import net.livaddons.feature.FeatureSettings;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @Redirect(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState livaddons$renderDioriteAsGrayGlass(RenderSectionRegion region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        if (FeatureSettings.dioriteToGlassEnabled()
                && (state.is(Blocks.DIORITE) || state.is(Blocks.POLISHED_DIORITE))) {
            return Blocks.GRAY_STAINED_GLASS.defaultBlockState();
        }
        return state;
    }
}
