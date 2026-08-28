package ru.krotarnya.diasync2.wear;

interface WearSnapshotStore {
    byte[] read();

    boolean write(byte[] payload);
}
