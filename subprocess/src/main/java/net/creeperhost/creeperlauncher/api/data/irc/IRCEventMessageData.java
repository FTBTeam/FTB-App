package net.creeperhost.creeperlauncher.api.data.irc;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class IRCEventMessageData extends BaseData {
    private final String target;
    private final String message;
    private final String nick;

    public IRCEventMessageData(String target, String message, String nick) {
        this.type = "ircMessage";
        this.target = target;
        this.message = message;
        this.nick = nick;
    }
}
