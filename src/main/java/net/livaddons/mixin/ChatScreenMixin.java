package net.livaddons.mixin;

import net.livaddons.access.ChatCopyAccess;
import net.livaddons.feature.FeatureSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void livaddons$copyChatMessage(MouseButtonEvent event, boolean consumed,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureSettings.copyChatEnabled()) return;

        int mode = FeatureSettings.copyChatMode();
        boolean matches = switch (mode) {
            case 1 -> event.button() == 0 && event.hasShiftDown();
            case 2 -> event.button() == 1;
            default -> event.button() == 0 && !event.hasShiftDown();
        };
        if (!matches) return;

        Minecraft minecraft = Minecraft.getInstance();
        ChatComponent chat = minecraft.gui.getChat();
        String message = ((ChatCopyAccess) chat).livaddons$messageAt(
                event.x(), event.y(), minecraft.getWindow().getGuiScaledHeight());
        if (message == null || message.isBlank()) return;

        minecraft.keyboardHandler.setClipboard(message);
        cir.setReturnValue(true);
    }
}
