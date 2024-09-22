package net.creeperhost.creeperlauncher.accounts.data;

import java.util.List;

public class MicrosoftResponses {
    public static final class XboxLiveResponse {
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
}
