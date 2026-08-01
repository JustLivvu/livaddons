package net.livaddons.mixin;

import net.livaddons.util.VisualHeightHolder;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements VisualHeightHolder {
    @Unique
    private float livaddons$visualHeight = 1.0f;

    @Override
    public float livaddons$getVisualHeight() {
        return livaddons$visualHeight;
    }

    @Override
    public void livaddons$setVisualHeight(float height) {
        this.livaddons$visualHeight = height;
    }
}
