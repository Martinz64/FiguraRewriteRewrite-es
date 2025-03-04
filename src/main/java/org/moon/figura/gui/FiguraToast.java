package org.moon.figura.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.moon.figura.FiguraMod;
import org.moon.figura.config.Config;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.FiguraIdentifier;
import org.moon.figura.utils.ui.UIHelper;

import java.util.List;

public class FiguraToast implements Toast {

    private final ToastType type;
    private Component title, message;

    private long startTime;
    private boolean update;

    public FiguraToast(Component title, Component message, ToastType type) {
        this.type = type;
        update(title, message, false);
    }

    public void update(Component title, Component message, boolean update) {
        this.title = Component.empty().setStyle(type.style).append(title);
        this.message = message;
        this.update = update;
    }

    @Override
    public Visibility render(PoseStack stack, ToastComponent component, long startTime) {
        int time = Math.round(Config.TOAST_TIME.asFloat() * 1000);
        int titleTime = Math.round(Config.TOAST_TITLE_TIME.asFloat() * 1000);

        if (this.update) {
            if (startTime - this.startTime < time)
                Visibility.SHOW.playSound(component.getMinecraft().getSoundManager());
            this.startTime = startTime;
            this.update = false;
        }

        long timeDiff = startTime - this.startTime;

        UIHelper.setupTexture(type.texture);
        UIHelper.blit(stack, 0, 0, 0f, (int) ((FiguraMod.ticks / 5f) % type.frames + 1) * height(), width(), height(), width(), height() * type.frames);

        Font font = component.getMinecraft().font;
        if (this.message.getString().isBlank()) {
            renderText(this.title, font, stack, 0xFF);
        } else if (this.title.getString().isBlank()) {
            renderText(this.message, font, stack, 0xFF);
        } else {
            List<FormattedCharSequence> a = font.split(this.title, width() - type.spacing - 1);
            List<FormattedCharSequence> b = font.split(this.message, width() - type.spacing - 1);

            if (a.size() == 1 && b.size() == 1) {
                int y = Math.round(height() / 2f - font.lineHeight - 1);
                font.draw(stack, this.title, type.spacing, y, 0xFFFFFF);
                font.draw(stack, this.message, type.spacing, y * 2 + 4, 0xFFFFFF);
            } else if (timeDiff < titleTime) {
                renderText(this.title, font, stack, Math.round(Math.min(Math.max((titleTime - timeDiff) / 300f, 0), 1) * 255));
            } else {
                renderText(this.message, font, stack, Math.round(Math.min(Math.max((timeDiff - titleTime) / 300f, 0), 1) * 255));
            }
        }

        return timeDiff < time ? Visibility.SHOW : Visibility.HIDE;
    }

    public void renderText(Component text, Font font, PoseStack stack, int alpha) {
        List<FormattedCharSequence> list = font.split(text, width() - type.spacing - 1);
        if (list.size() == 1)
            font.draw(stack, text, type.spacing, Math.round(height() / 2f - font.lineHeight / 2f), 0xFFFFFF + (alpha << 24));
        else {
            int y = Math.round(height() / 2f - font.lineHeight - 1);
            for (int i = 0; i < list.size(); i++)
                font.draw(stack, list.get(i), type.spacing, y * (i + 1) + 4 * i, 0xFFFFFF + (alpha << 24));
        }
    }

    @Override
    public Object getToken() {
        return this.type;
    }

    @Override
    public int width() {
        return type.width;
    }

    @Override
    public int height() {
        return 32;
    }

    //new toast
    public static void sendToast(Object title) {
        sendToast(title, Component.empty());
    }

    public static void sendToast(Object title, ToastType type) {
        sendToast(title, Component.empty(), type);
    }

    public static void sendToast(Object title, Object message) {
        sendToast(title, message, ToastType.DEFAULT);
    }

    public static void sendToast(Object title, Object message, ToastType type) {
        Component text = title instanceof Component t ? t : Component.translatable(title.toString());
        Component text2 = message instanceof Component m ? m : Component.translatable(message.toString());

        if (type == ToastType.DEFAULT && Config.EASTER_EGGS.asBool()) {
            if (FiguraMod.CHEESE_DAY || Math.random() < 0.0001)
                type = ToastType.CHEESE;
            else if (FiguraMod.DATE.getDayOfMonth() == 21 && FiguraMod.DATE.getMonthValue() == 9)
                type = ToastType.FRAN;
        }

        ToastComponent toasts = Minecraft.getInstance().getToasts();
        FiguraToast toast = toasts.getToast(FiguraToast.class, type);

        if (toast != null)
            toast.update(text, text2, true);
        else
            toasts.addToast(new FiguraToast(text, text2, type));
    }

    public enum ToastType {
        DEFAULT(new FiguraIdentifier("textures/gui/toast/default.png"), 4, 160, 31, 0x55FFFF),
        WARNING(new FiguraIdentifier("textures/gui/toast/warning.png"), 4, 160, 31, 0xFFFF00),
        ERROR(new FiguraIdentifier("textures/gui/toast/error.png"), 4, 160, 31, 0xFF0000),
        CHEESE(new FiguraIdentifier("textures/gui/toast/cheese.png"), 1, 160, 31, ColorUtils.Colors.CHEESE.hex),
        FRAN(new FiguraIdentifier("textures/gui/toast/fran.png"), 4, 160, 31, ColorUtils.Colors.FRAN_PINK.hex);

        private final ResourceLocation texture;
        private final int frames;
        private final Style style;
        private final int width, spacing;

        ToastType(ResourceLocation texture, int frames, int width, int spacing, int color) {
            this.texture = texture;
            this.frames = frames;
            this.width = width;
            this.spacing = spacing;
            this.style = Style.EMPTY.withColor(color);
        }
    }
}
