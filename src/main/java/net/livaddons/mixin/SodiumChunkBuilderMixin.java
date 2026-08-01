package net.livaddons.mixin;

import net.livaddons.feature.FeatureSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies the diorite model replacement inside Sodium's chunk mesher. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask")
public abstract class SodiumChunkBuilderMixin {
    @ModifyVariable(method = "execute", at = @At(value = "STORE"), ordinal = 0, require = 0)
    private BlockState livaddons$renderDioriteAsGlass(BlockState state) {
        if (FeatureSettings.dioriteToGlassEnabled()
                && (state.is(Blocks.DIORITE) || state.is(Blocks.POLISHED_DIORITE))) {
            return Blocks.GRAY_STAINED_GLASS.defaultBlockState();
        }
        return state;
    }
}
