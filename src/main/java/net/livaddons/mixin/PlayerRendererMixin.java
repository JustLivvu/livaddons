package net.livaddons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.util.HypixelUtil;
import net.livaddons.util.VisualHeightHolder;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void onExtractState(Avatar entity, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity != null && state instanceof VisualHeightHolder holder) {
            UUID uuid = entity.getUUID();
            PlayerCosmeticData data = PlayerDataManager.getInstance().getCosmeticData(uuid);
            if (data != null && data.visualHeight >= 0.5f && data.visualHeight <= 2.0f) {
                holder.livaddons$setVisualHeight(data.visualHeight);
            } else {
                holder.livaddons$setVisualHeight(1.0f);
            }
        }
    }

    @Inject(
        method = "scale",
        at = @At("TAIL")
    )
    private void onScale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (state instanceof VisualHeightHolder holder) {
            float scale = holder.livaddons$getVisualHeight();
            if (scale != 1.0f) {
                poseStack.scale(scale, scale, scale);
            }
        }
    }
}
