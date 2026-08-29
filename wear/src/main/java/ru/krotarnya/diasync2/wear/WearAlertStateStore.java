package ru.krotarnya.diasync2.wear;

interface WearAlertStateStore {
    WearAlertState read();

    boolean write(WearAlertState state);
}
