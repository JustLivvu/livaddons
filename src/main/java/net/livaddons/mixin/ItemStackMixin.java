package net.livaddons.mixin;

import net.livaddons.util.ComponentReplacer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void onGetHoverName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            Component replaced = ComponentReplacer.replaceInComponent(original);
            if (replaced != original) {
                cir.setReturnValue(replaced);
            }
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void onGetTooltipLines(Item.TooltipContext context, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> originalLines = cir.getReturnValue();
        if (originalLines != null && !originalLines.isEmpty()) {
            List<Component> newLines = new ArrayList<>(originalLines.size());
            boolean modified = false;
            for (Component line : originalLines) {
                Component replaced = ComponentReplacer.replaceInComponent(line);
                if (replaced != line) {
                    modified = true;
                }
                newLines.add(replaced);
            }
            if (modified) {
                cir.setReturnValue(newLines);
            }
        }
    }
}
