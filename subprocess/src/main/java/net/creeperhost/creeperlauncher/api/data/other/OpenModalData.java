package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.List;
import java.util.UUID;

public class OpenModalData extends BaseData
{
    private final String title;
    private final String message;
    public final List<ModalButton> buttons;
    public static OpenModalData currentlyOpenModal = null;
    public String id;

    public OpenModalData(String title, String message, List<ModalButton> buttons) {
        this.type = "openModal";
        this.title = title;
        this.message = message;
        this.buttons = buttons;
        this.id = UUID.randomUUID().toString();
    }

    public static void openModal(String title, String message, List<ModalButton> modalButtons) {
        currentlyOpenModal = new OpenModalData(title, message, modalButtons);
        WebSocketHandler.sendMessage(currentlyOpenModal);
    }

    public static class ModalButton
    {
        public final String message;
        private final String name;
        private final String type;
        public final transient Runnable callback;

        public ModalButton(String name, String type, Runnable callback) {
            this(UUID.randomUUID().toString(), name, type, callback);
        }

        public ModalButton(String message, String name, String type, Runnable callback) {
            this.message = message;
            this.name = name;
            this.type = type;
            this.callback = callback;
        }
    }

    public static class ModalCallbackData extends BaseData
    {
        public String id;
        public String message;
    }
}
