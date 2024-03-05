package net.creeperhost.creeperlauncher.api.handlers.other.minetogether;

import java.util.List;
import java.util.Map;

public class MineTogetherAccount {
    public String id;
    public String username;
    public String firstName;
    public String lastName;
    public String email;
    public Map<String, List<String>> attributes;
    public Account[] accounts;
    public String mtHash;
    public String fullMTHash;
    public ActivePlan activePlan;
        
    public static class Account {
        public String identityProvider;
        public String userId;
        public String userName;
        public String mcName;
        public String accountKey;
        public String accountSecret;
        
        public String toString() {
            return "Account{" +
                "identityProvider='" + identityProvider + '\'' +
                ", userID=" + userId +
                ", userName='" + userName + '\'' +
                ", mcName='" + mcName + '\'' +
                ", accountKey='" + accountKey + '\'' +
                ", accountSecret='" + accountSecret + '\'' +
                '}';
        }
    }

    public static class ActivePlan {
        public long id;
        public long orderid;
        public long pid;
        public String name;
        public String paymentMethod;
        public String paymentMethodActual;
        public String nextDueDate;
        public String status;
        public CustomFields customFields;
    }

    public static class CustomFields {
        public Customfield[] customfield;
    }

    public static class Customfield {
        public long id;
        public String name;
        public String translatedName;
        public String value;
    }
}
