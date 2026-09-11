package ru.krotarnya.diasync2.master;

public enum MasterEventState {
    PENDING,
    IN_FLIGHT,
    DELIVERED,
    BLOCKED
}
