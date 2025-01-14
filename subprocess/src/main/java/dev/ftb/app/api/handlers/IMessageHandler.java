package dev.ftb.app.api.handlers;

import dev.ftb.app.api.data.BaseData;

public interface IMessageHandler<T extends BaseData>
{
    @SuppressWarnings("unchecked")
    default void handle(Object data)
    {
        handle((T) data);
    }

    void handle(T data);
}
