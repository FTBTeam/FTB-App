package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.HashMap;

public class SettingsConfigureData extends BaseData
{

    public HashMap<String, String> settingsInfo; //TODO: second parameter should be something other than String maybe

    public static class Reply extends BaseData
    {
        String status;

        public Reply(SettingsConfigureData data, String status)
        {
            type = "saveSettingsReply";
            this.requestId = data.requestId;
            this.status = "success";
        }
    }
}
