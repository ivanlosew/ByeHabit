package com.example.byehabit.ui.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.byehabit.R;
import com.example.byehabit.ui.MainActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class HabitInfo extends AppCompatActivity {

    private int currentFails, id;
    private TextView habitLabel, habitDays, failsValue, lastFailValue;
    private Button success;
    private HabitAdd.DBHelper dbHelper;
    private SharedPreferences sPref;
    private TextView titleInfo;
    private static final String APP_PREFERENCES_GENDER = "GENDER";
    private SharedPreferences achPref; //SP ачивок
    private SharedPreferences nicknamePref; //никнейм
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_habit);

        habitLabel = findViewById(R.id.habitLabel);
        habitDays = findViewById(R.id.habitDays);
        failsValue = findViewById(R.id.failsValue);
        lastFailValue = findViewById(R.id.lastFailValue);
        success = findViewById(R.id.success);
        success.setOnClickListener(habitSuccess);

        dbHelper = new HabitAdd.DBHelper(getApplicationContext()); //дб хелпер
        nicknamePref = Objects.requireNonNull(getApplicationContext()).getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);

        //меняем заголовок если Ж
        sPref = Objects.requireNonNull(getApplicationContext()).getSharedPreferences("sPref", Context.MODE_PRIVATE);
        if (sPref.getString(APP_PREFERENCES_GENDER, "").equals("FEMALE")) {
            titleInfo = findViewById(R.id.titleInfo);
            titleInfo.setText(R.string.title_habits_w);
            String successButtonText = "Нажми, если не сорвалась";
            success.setText(successButtonText);
        }

        //получаем инфу от активности привычек
        Bundle bundle;
        bundle = this.getIntent().getExtras();

        //ставим название привычки
        String newLabel = Objects.requireNonNull(bundle).getString("Label");
        habitLabel.setText(newLabel);

        //если сегодня уже кнопка нажималась, убираем кнопку
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        if (formatForDateNow.format(new Date(System.currentTimeMillis())).equals(formatForDateNow.format(new Date(bundle.getLong("LastSuc")))))
            success.setVisibility(View.GONE);

        //ставим сколько дней назад добавлена привычка
        long created = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(bundle).getString("Days")));
        int days = (int) (System.currentTimeMillis() / 1000 - created) / 60 / 60 / 24;
        String newDays = days + declensionDays(days) + " назад";
        habitDays.setText(newDays);

        //ставим сколько провалов и когда последний
        currentFails = Objects.requireNonNull(bundle).getInt("Fails");
        failsValue.setText(String.valueOf(currentFails));
        lastFailValue.setText(Objects.requireNonNull(bundle).getString("LastFail"));

        //получение ачивки Десятка
        achPref = Objects.requireNonNull(getApplicationContext()).getSharedPreferences("achPref", Context.MODE_PRIVATE);
        if (days >= 10 && !achPref.getBoolean("Десятка", false)) {
            SharedPreferences.Editor editor2 = achPref.edit();
            editor2.putBoolean("Десятка", true);
            editor2.apply();
            sendNotificationAchievement(3);
            addAchievementsInFirebase("Десятка");
        }

    }

    //обрабатываем
    View.OnClickListener habitSuccess = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //убираем кнопку
            success.setVisibility(View.GONE);

            //добавлем инфу в бд
            ContentValues cv = new ContentValues();
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            cv.put("lastSuc", System.currentTimeMillis());
            db.update("habits", cv, "id = ?", new String[] {String.valueOf(id)});

            db.close(); //закрывем бд
        }
    };

    //определение склонения слова "день"
    private String declensionDays (int n) {
        if (n == 11 || n == 12 || n == 13 || n == 14)
            return " дней";
        switch (n % 10) {
            case 0:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return " дней";
            case 1:
                return " день";
            case 2:
            case 3:
            case 4:
                return " дня";
        }
        return null;
    }

    //отправляем уведомление о новом достижении
    private void sendNotificationAchievement(int achievementId) {
        Intent resultIntent = new Intent(HabitInfo.this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("achievements", "Достижения", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(Objects.requireNonNull(getApplicationContext()));
            notificationManager.createNotificationChannel(notificationChannel);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(Objects.requireNonNull(getApplicationContext()), "achievements")
                    .setSmallIcon(R.drawable.ic_achievements_24dp)
                    .setContentTitle("Новое достижение")
                    .setContentText("Откройте вкладку «Достижения», чтобы посмотреть")
                    .setAutoCancel(true)
                    .setContentIntent(resultPendingIntent);
            Notification notification = builder.build();
            notificationManager.notify(achievementId, notification);
        }
        else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(Objects.requireNonNull(getApplicationContext()));
            NotificationCompat.Builder builder = new NotificationCompat.Builder(Objects.requireNonNull(getApplicationContext()))
                    .setSmallIcon(R.drawable.ic_achievements_24dp)
                    .setContentTitle("Новое достижение")
                    .setContentText("Откройте вкладку «Достижения», чтобы посмотреть")
                    .setAutoCancel(true)
                    .setContentIntent(resultPendingIntent);
            Notification notification = builder.build();
            notificationManager.notify(achievementId, notification);
        }
    }

    //добавить ачивку в онлайн БД
    private void addAchievementsInFirebase (String achievement) {
        database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        database.child("achievements").child(achievement).setValue(true);
    }
}

