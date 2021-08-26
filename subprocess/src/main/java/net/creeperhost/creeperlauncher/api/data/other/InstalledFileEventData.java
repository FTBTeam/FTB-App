package net.creeperhost.creeperlauncher.api.data.other;

import java.util.concurrent.ConcurrentHashMap;
import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.HashMap;

public class InstalledFileEventData extends BaseData {
    public static class Reply extends BaseData {
        ConcurrentHashMap<Long, String> files;
        public Reply(ConcurrentHashMap<Long, String> files) {
            this.files = files;
            this.type = "installedFileEventDataReply";
        }
    }
}
