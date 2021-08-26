package net.creeperhost.creeperlauncher.api.data.irc;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class IRCEventMessageData extends BaseData {
    private final String message;
    private final String nick;

    public IRCEventMessageData(String message, String nick) {
        this.type = "ircMessage";
        this.message = message;
        this.nick = nick;
    }
}
