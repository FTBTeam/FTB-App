package net.creeperhost.creeperlauncher.install.tasks;

import java.io.IOException;

/**
 * Created by covers1624 on 22/11/22.
 */
public class DownloadFailedException extends IOException {

    public DownloadFailedException(String message) {
        super(message);
    }
}
