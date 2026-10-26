package dev.ftb.app.api.handlers;

import dev.ftb.app.api.data.BaseData;

public interface IMessageHandler<T extends BaseData>
{
    @SuppressWarnings("unchecked")
    default void handle(MessageContext context, Object data)
    {
        handle(context, (T) data);
    }

    void handle(MessageContext context, T data);
}
