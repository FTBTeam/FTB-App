package net.creeperhost.creeperlauncher.api.handlers.other.minetogether;

import com.google.gson.annotations.SerializedName;

import java.util.StringJoiner;

public class MineTogetherProfile {
    public Hashes hash;
    public String friendCode;
    public Chat chat;
    public boolean cached;
    public String display;
    public boolean hasAccount;
    public boolean premium;

    @Override
    public String toString() {
        return new StringJoiner(", ", MineTogetherProfile.class.getSimpleName() + "[", "]")
            .add("hash=" + hash)
            .add("friendCode='" + friendCode + "'")
            .add("chat=" + chat)
            .add("cached=" + cached)
            .add("display='" + display + "'")
            .add("hasAccount=" + hasAccount)
            .add("premium=" + premium)
            .toString();
    }

    public static class Chat {
        public ChatHash hash;
        public boolean online;

        @Override
        public String toString() {
            return new StringJoiner(", ", Chat.class.getSimpleName() + "[", "]")
                .add("hash=" + hash)
                .add("online=" + online)
                .toString();
        }
    }
    
    public static class ChatHash {
        public String medium;
        public String hashShort;

        @Override
        public String toString() {
            return new StringJoiner(", ", ChatHash.class.getSimpleName() + "[", "]")
                .add("medium='" + medium + "'")
                .add("hashShort='" + hashShort + "'")
                .toString();
        }
    }

    public static class Hashes {
        @SerializedName("long")
        public String hashLong;

        @Override
        public String toString() {
            return new StringJoiner(", ", Hashes.class.getSimpleName() + "[", "]")
                .add("hashLong='" + hashLong + "'")
                .toString();
        }
    }
}
