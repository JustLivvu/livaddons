package net.livaddons.mixin;

import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.util.ComponentReplacer;
import net.livaddons.util.HypixelUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNametagMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!HypixelUtil.isOnHypixel()) return;

        Player player = (Player) (Object) this;
        PlayerCosmeticData data = PlayerDataManager.getInstance().getCosmeticData(player.getUUID());

        if (data != null && data.customNick != null && !data.customNick.trim().isEmpty()) {
            Component original = cir.getReturnValue();
            if (original != null) {
                cir.setReturnValue(ComponentReplacer.replacePlayerName(original, data));
            }
        }
    }
}
