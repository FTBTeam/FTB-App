package net.creeperhost.creeperlauncher.api.handlers.irc;

import net.creeperhost.creeperlauncher.api.data.irc.IRCQuitRequestData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.minetogether.lib.chat.irc.IrcHandler;

public class IRCQuitRequestHandler implements IMessageHandler<IRCQuitRequestData> {
    @Override
    public void handle(IRCQuitRequestData data) {
        IrcHandler.stop(false);
    }
}
