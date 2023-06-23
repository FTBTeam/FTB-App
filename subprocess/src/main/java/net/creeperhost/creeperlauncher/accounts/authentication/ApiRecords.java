package net.creeperhost.creeperlauncher.accounts.authentication;

import java.util.List;

public class ApiRecords {
    public static class Responses {
        static final class XblAuth {
            public String IssueInstant;
            public String NotAfter;
            public String Token;
            public XblDisplayClaims DisplayClaims;

            static class XblDisplayClaims {
                public List<XblDisplayClaim> xui;
            }

            static class XblDisplayClaim {
                public String uhs;
            }
        }

        public static class LoginWithXbox {
            public String username;
            public List<String> roles;
            public String access_token;
            public String token_type;
            public int expires_in;
        }

        static final class Entitlements {
            public List<Entitlement> items;
            public String signature;
            public String keyId;

            static class Entitlement {
                public String name;
                public String signature;
            }
        }
        
        public static final class Migration {
            public String feature;
            public boolean rollout;

            public Migration(String feature, boolean rollout) {
                this.feature = feature;
                this.rollout = rollout;
            }
        }
    }

    static class Requests {
        public static class XblAuth {
            public XblAuthProperties Properties;
            public String RelyingParty;
            public String TokenType;

            public XblAuth(XblAuthProperties properties, String relyingParty, String tokenType) {
                Properties = properties;
                RelyingParty = relyingParty;
                TokenType = tokenType;
            }
        }

        public static class XblAuthProperties {
            public String AuthMethod;
            public String SiteName;
            public String RpsTicket;

            public XblAuthProperties(String authMethod, String siteName, String rpsTicket) {
                AuthMethod = authMethod;
                SiteName = siteName;
                RpsTicket = rpsTicket;
            }
        }

        public static class XstsAuth {
            public XstsAuthProperties Properties;
            public String RelyingParty;
            public String TokenType;

            public XstsAuth(XstsAuthProperties properties, String relyingParty, String tokenType) {
                Properties = properties;
                RelyingParty = relyingParty;
                TokenType = tokenType;
            }
        }

        public static class XstsAuthProperties {
            public String SandboxId;
            public List<String> UserTokens;

            public XstsAuthProperties(String sandboxId, List<String> userTokens) {
                SandboxId = sandboxId;
                UserTokens = userTokens;
            }
        }

        public static class LoginWithXbox {
            public String xtoken;
            public String platform;

            public LoginWithXbox(String xtoken, String platform) {
                this.xtoken = xtoken;
                this.platform = platform;
            }
        }
    }
}