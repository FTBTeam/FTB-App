package dev.ftb.app.install.tasks;

import java.io.IOException;

public class DownloadFailedException extends IOException {

    public DownloadFailedException(String message) {
        super(message);
    }
}
