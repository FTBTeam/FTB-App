package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.HashMap;

public class GetJavasData extends BaseData {
    public static class Reply extends BaseData {
        HashMap<String, String> javas;
        public Reply(GetJavasData data, HashMap<String, String> javas) {
            this.javas = javas;
            this.requestId = data.requestId;
        }
    }
}
