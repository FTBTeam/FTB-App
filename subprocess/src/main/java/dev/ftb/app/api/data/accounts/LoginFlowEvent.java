package dev.ftb.app.api.data.accounts;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.structs.accounts.AccountProvider;
import org.jspecify.annotations.Nullable;

/// Pushed to the frontend while a login finishes in the background
public class LoginFlowEvent extends BaseData {
    public static final String TYPE = "event.accounts.login";

    public final AccountProvider provider;

    public final String flowId;
    public final LoginStatus status;

    public final @Nullable LoginStep step;
    public final @Nullable String message;
    public final @Nullable String accountName;

    private LoginFlowEvent(AccountProvider provider, String flowId, LoginStatus status, @Nullable LoginStep step, @Nullable String message, @Nullable String accountName) {
        this.type = TYPE;
        this.provider = provider;
        this.flowId = flowId;
        this.status = status;
        this.step = step;
        this.message = message;
        this.accountName = accountName;
    }

    public static LoginFlowEvent working(AccountProvider provider, String flowId, LoginStep step) {
        return new LoginFlowEvent(provider, flowId, LoginStatus.WORKING, step, null, null);
    }

    public static LoginFlowEvent complete(AccountProvider provider, String flowId, String accountName) {
        return new LoginFlowEvent(provider, flowId, LoginStatus.COMPLETE, null, null, accountName);
    }

    public static LoginFlowEvent failed(AccountProvider provider, String flowId, String message) {
        return new LoginFlowEvent(provider, flowId, LoginStatus.FAILED, null, message, null);
    }

    public enum LoginStatus {
        WORKING,
        COMPLETE,
        FAILED
    }

    public enum LoginStep {
        EXCHANGING_CODE,
        XBOX,
        XSTS,
        MINECRAFT,
        PROFILE,
        SAVING
    }
}
