package dev.ftb.app.api.data.accounts;

import dev.ftb.app.api.data.BaseData;

/// Pushed to the frontend when the backend changes accounts by itself, such as a refresh at startup signing one out.
/// The frontend should fetch the accounts again.
public class AccountsChangedEvent extends BaseData {
    public AccountsChangedEvent() {
        this.type = "event.accounts.changed";
    }
}
