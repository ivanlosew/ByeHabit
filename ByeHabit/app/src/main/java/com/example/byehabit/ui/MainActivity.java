package com.example.byehabit.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.byehabit.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sPref;
    private static final String APP_PREFERENCES_MODE = "MODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        //подключаемся к AppCenter (HockeyApp) для доп. анализа
        AppCenter.start(getApplication(), "dfee5a0c-687a-4216-a5fa-3bab288ae166", Analytics.class, Crashes.class);
        AppCenter.setLogLevel(Log.VERBOSE);

        //включаем тему при запуске
        sPref = Objects.requireNonNull(getApplicationContext()).getSharedPreferences("sPref", Context.MODE_PRIVATE);
        String MODE = sPref.getString(APP_PREFERENCES_MODE, "");
        switch (MODE) {
            case "LIGHT":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "DARK":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "AUTO":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "BATTERY":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
        }
    }

    //меню (кнопка открытия настроек и справки)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Настройки");
        menu.add(0, 2, 0, "Справка");
        return super.onCreateOptionsMenu(menu);
    }

    //открываем настройки или справку
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case 1:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case 2:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //при возврате из активити вызываем onActivityResult фрагмента
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
