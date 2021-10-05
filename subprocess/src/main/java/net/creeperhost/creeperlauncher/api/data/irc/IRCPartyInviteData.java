package net.creeperhost.creeperlauncher.api.data.irc;

import net.creeperhost.creeperlauncher.api.data.friends.FriendData;
import net.creeperhost.minetogether.lib.chat.data.Profile;

public class IRCPartyInviteData extends FriendData {
    public IRCPartyInviteData(Profile profile) {
        super(profile);
        this.type = "ircPartyInvite";
    }
}
