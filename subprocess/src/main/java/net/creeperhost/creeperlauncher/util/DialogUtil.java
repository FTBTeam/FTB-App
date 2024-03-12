package net.creeperhost.creeperlauncher.util;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DialogUtil {

    public static boolean confirmDialog(String title, String body) {
        return confirmDialog(title, "Yes", "No", body);
    }

    public static boolean confirmDialog(String title, String confirmText, String denyText, String body) {
        AtomicBoolean result = new AtomicBoolean();

        OpenModalData.openModal(title, body, List.of(
                new OpenModalData.ModalButton(confirmText, "success", () -> {
                    result.set(true);
                    synchronized (result) {
                        WebSocketHandler.sendMessage(new CloseModalData());
                        result.notify();
                    }
                }),
                new OpenModalData.ModalButton(denyText, "danger", () -> {
                    result.set(false);
                    synchronized (result) {
                        WebSocketHandler.sendMessage(new CloseModalData());
                        result.notify();
                    }
                })
        ));

        try {
            synchronized (result) {
                result.wait();
            }
        } catch (InterruptedException ignored) {
        }

        return result.get();
    }

    public static void okDialog(String title, String body) {
        Object lock = new Object();

        OpenModalData.openModal(title, body, List.of(
                new OpenModalData.ModalButton("Ok", "success", () -> {
                    synchronized (lock) {
                        WebSocketHandler.sendMessage(new CloseModalData());
                        lock.notify();
                    }
                })
        ));

        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException ignored) {
        }
    }
}
