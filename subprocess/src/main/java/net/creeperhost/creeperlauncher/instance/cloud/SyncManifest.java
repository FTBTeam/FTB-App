package net.creeperhost.creeperlauncher.instance.cloud;

/**
 * Created by covers1624 on 22/3/23.
 */
public class SyncManifest {

    public long lastSync;
    public State state;

    public enum State {
        SYNCING,
        SYNCED
    }
}
