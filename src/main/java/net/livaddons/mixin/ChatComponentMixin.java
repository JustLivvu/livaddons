package net.livaddons.mixin;

import net.livaddons.access.ChatCopyAccess;
import net.livaddons.util.ComponentReplacer;
import net.livaddons.feature.TerminalsHud;
import net.livaddons.feature.DungeonFinishSong;
import net.livaddons.feature.PartyCommands;
import net.livaddons.feature.LeapAlert;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatCopyAccess {
    @Shadow private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private double getScale() { throw new AssertionError(); }
    @Shadow private int getWidth() { throw new AssertionError(); }
    @Shadow private int getLineHeight() { throw new AssertionError(); }

    @ModifyVariable(method = "addClientSystemMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddClientSystemMessage(Component message) {
        TerminalsHud.onGameMessage(message);
        DungeonFinishSong.onChatMessage(message);
        PartyCommands.onChatMessage(message);
        LeapAlert.onChatMessage(message);
        return ComponentReplacer.replaceInComponent(message);
    }

    @ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddServerSystemMessage(Component message) {
        TerminalsHud.onGameMessage(message);
        DungeonFinishSong.onChatMessage(message);
        PartyCommands.onChatMessage(message);
        LeapAlert.onChatMessage(message);
        return ComponentReplacer.replaceInComponent(message);
    }

    @ModifyVariable(method = "addPlayerMessage", at = @At("HEAD"), argsOnly = true)
    private Component onAddPlayerMessage(Component message) {
        TerminalsHud.onGameMessage(message);
        DungeonFinishSong.onChatMessage(message);
        PartyCommands.onChatMessage(message);
        LeapAlert.onChatMessage(message);
        return ComponentReplacer.replaceInComponent(message);
    }

    @Override
    public String livaddons$messageAt(double mouseX, double mouseY, int screenHeight) {
        double scale = getScale();
        if (mouseX < 0 || mouseX > getWidth()) return null;

        int bottom = (int) Math.floor((screenHeight - 40) / scale);
        int cursorY = (int) Math.floor(mouseY / scale);
        int distanceFromBottom = bottom - cursorY;
        if (distanceFromBottom < 0) return null;

        int line = distanceFromBottom / Math.max(1, getLineHeight()) + chatScrollbarPos;
        if (line < 0 || line >= trimmedMessages.size()) return null;
        return trimmedMessages.get(line).parent().content().getString();
    }
}
