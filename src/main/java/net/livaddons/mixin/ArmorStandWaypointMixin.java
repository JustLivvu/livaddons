package net.livaddons.mixin;

import net.livaddons.feature.TerminalWaypoints;
import net.livaddons.util.HypixelUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ArmorStandWaypointMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void livaddons$terminalWaypoint(CallbackInfoReturnable<Component> cir) {
        if (!HypixelUtil.isOnHypixel()) return;
        if (!((Object) this instanceof ArmorStand armorStand)) return;
        Component replacement = TerminalWaypoints.waypointName(armorStand, cir.getReturnValue());
        if (replacement != null) cir.setReturnValue(replacement);
    }
}
