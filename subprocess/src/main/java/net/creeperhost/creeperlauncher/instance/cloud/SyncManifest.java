package net.creeperhost.creeperlauncher.instance.cloud;

/**
 * Created by covers1624 on 22/3/23.
 */
public class SyncManifest {

    public long lastSync;
    public State state;

    public SyncManifest() {
    }

    public SyncManifest(long lastSync, State state) {
        this.lastSync = lastSync;
        this.state = state;
    }

    public enum State {
        SYNCING,    // An FTBA instance is currently syncing this instance.
        UNFINISHED_UP, // An FTBA instance 'finished' the sync process, but it failed.
        UNFINISHED_DOWN, // An FTBA instance 'finished' the sync process, but it failed.
        SYNCED,     // The cloud is in a fully synced state.
    }
}
