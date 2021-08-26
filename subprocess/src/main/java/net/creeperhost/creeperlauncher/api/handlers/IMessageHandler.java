package net.creeperhost.creeperlauncher.api.handlers;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public interface IMessageHandler<T extends BaseData>
{
    @SuppressWarnings("unchecked")
    default void handle(Object data)
    {
        handle((T) data);
    }

    void handle(T data);
}
