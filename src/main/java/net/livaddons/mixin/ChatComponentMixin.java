package net.livaddons.mixin;

import net.livaddons.util.ComponentReplacer;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(method = "addClientSystemMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddClientSystemMessage(Component message) {
        return ComponentReplacer.replaceInComponent(message);
    }

    @ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddServerSystemMessage(Component message) {
        return ComponentReplacer.replaceInComponent(message);
    }

    @ModifyVariable(method = "addPlayerMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddPlayerMessage(Component message) {
        return ComponentReplacer.replaceInComponent(message);
    }
}
