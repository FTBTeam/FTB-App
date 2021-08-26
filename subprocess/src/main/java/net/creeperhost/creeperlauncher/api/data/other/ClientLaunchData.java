package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class ClientLaunchData extends BaseData {
    public static class Reply extends BaseData {
        String messageType;
        String message;
        String instance;
        Object clientData;
        public Reply(String instance, String messageType, String message){
             this(instance, messageType, message, null);
        }
        public Reply(String instance, String messageType, Object clientData){
            this(instance, messageType, null, clientData);
        }
        public Reply(String instance, String messageType, String message, Object clientData)
        {
            this.messageType = messageType;
            this.message = message;
            this.clientData = clientData;
            this.instance = instance;
            this.type = "clientLaunchData";
        }
    }
}
