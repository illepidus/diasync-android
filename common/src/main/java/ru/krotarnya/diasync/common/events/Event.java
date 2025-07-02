package ru.krotarnya.diasync.common.events;

import org.greenrobot.eventbus.EventBus;

public interface Event<T> {
    default void post() {
        EventBus.getDefault().post(this);
    }
}
