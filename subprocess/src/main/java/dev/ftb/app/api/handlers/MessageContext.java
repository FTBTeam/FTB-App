package dev.ftb.app.api.handlers;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.services.Services;

public record MessageContext(
    Services services
) {
    public void reply(BaseData data) {
        this.services.rcpApi().sendMessage(data);
    }
}
