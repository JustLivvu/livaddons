package net.livaddons.mixin;

import net.livaddons.feature.DungeonMapHud;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMapMixin {
    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void livaddons$captureDungeonMap(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        DungeonMapHud.onMapPacket(packet);
    }
}
