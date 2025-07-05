package ru.krotarnya.diasync.common.repository;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.Optional;

@Dao
public interface SettingsDao {
    @Query("SELECT * FROM settings " +
            "ORDER BY id DESC " +
            "LIMIT 1")
    Optional<Settings> find();

    default Settings get() {
        return find().orElseGet(Settings::getDefault);
    }
}