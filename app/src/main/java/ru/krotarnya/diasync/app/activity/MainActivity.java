package ru.krotarnya.diasync.app.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import ru.krotarnya.diasync.app.R;
import ru.krotarnya.diasync.app.fragment.SettingsFragment;
import ru.krotarnya.diasync.common.service.DataSyncService;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            Fragment settingsFragment = new SettingsFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, settingsFragment);
            transaction.commit();
        }

        Intent serviceIntent = new Intent(this, DataSyncService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
}