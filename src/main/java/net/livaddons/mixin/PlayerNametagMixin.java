package net.livaddons.mixin;

import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.util.HypixelUtil;
import net.livaddons.util.TextGradientUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNametagMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!HypixelUtil.isOnHypixel()) return;

        Player player = (Player) (Object) this;
        PlayerCosmeticData data = PlayerDataManager.getInstance().getCosmeticData(player.getUUID());

        if (data != null && data.customNick != null && !data.customNick.trim().isEmpty()) {
            MutableComponent customName = TextGradientUtil.buildGradientText(
                    data.customNick,
                    data.colorStart,
                    data.colorEnd,
                    data.isBold,
                    data.isItalic
            );
            cir.setReturnValue(customName);
        }
    }
}
