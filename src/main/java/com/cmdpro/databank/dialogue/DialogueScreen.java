package com.cmdpro.databank.dialogue;

import com.cmdpro.databank.dialogue.styles.DialogueStyleManager;
import com.cmdpro.databank.networking.ModMessages;
import com.cmdpro.databank.networking.packet.CloseDialogueC2SPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class DialogueScreen extends Screen {
    public DialogueInstance instance;
    public DialogueScreen(DialogueInstance instance) {
        super(Component.empty());
        this.instance = instance;
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            DialogueStyleManager.styles.get(instance.entry.style).changeEntry(instance, null, instance.entry.id);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            if (DialogueStyleManager.styles.get(instance.entry.style).mouseClick(instance, event.x(), event.y(), event.button())) {
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            if (DialogueStyleManager.styles.get(instance.entry.style).mouseDrag(instance, event.x(), event.y(), event.button(), dx, dy)) {
                return true;
            }
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            if (DialogueStyleManager.styles.get(instance.entry.style).mouseRelease(instance, event.x(), event.y(), event.button())) {
                return true;
            }
        }

        return super.mouseReleased(event);
    }

    public void changeEntry(String from, String to) {
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            DialogueStyleManager.styles.get(instance.entry.style).changeEntry(instance, from, to);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (instance != null && instance.entry != null && instance.entry.style != null) {
            DialogueStyle.render(instance.entry.style, instance, graphics, mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (instance != null) {
            double last = instance.ticksOnEntry;
            instance.ticksOnEntry += instance.entry != null ? instance.entry.speed : 1;
            if (instance.entry != null && instance.entry.style != null) {
                DialogueStyleManager.styles.get(instance.entry.style).tick(instance, last, instance.ticksOnEntry);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        ModMessages.sendToServer(new CloseDialogueC2SPacket());
    }
}
