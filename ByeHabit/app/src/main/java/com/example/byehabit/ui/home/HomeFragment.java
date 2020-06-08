package com.example.byehabit.ui.home;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.byehabit.R;
import com.example.byehabit.ui.achievements.AchievementsFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView titleHome; //заголовок экрана
    private ImageButton newHabit; //кнопка добавления привычка
    private ImageButton info1, info2, info3; //кнопки info
    private TextView habitLabel1, habitLabel2, habitLabel3; //заголовки привычек
    private TextView habitSep1, habitSep2, habitSep3; //разделители
    private LinearLayout layout1, layout2, layout3;
    private HabitAdd.DBHelper dbHelper; //дб хелпер
    private SharedPreferences sPref;
    private static final String APP_PREFERENCES_GENDER = "GENDER";
    private SharedPreferences achPref; //SP ачивок
    private View root;

    //для firebase
    private SharedPreferences nicknamePref; //никнейм
    private DatabaseReference database;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new HabitAdd.DBHelper(getContext()); //дб хелпер
        achPref = requireContext().getSharedPreferences("achPref", Context.MODE_PRIVATE);
        nicknamePref = requireContext().getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);

        //лэйауты с привычками
        layout1 = root.findViewById(R.id.layout1);
        layout2 = root.findViewById(R.id.layout2);
        layout3 = root.findViewById(R.id.layout3);

        //кнопка добавления привычки
        newHabit = root.findViewById(R.id.newHabit);
        newHabit.setOnClickListener(adding);

        //кнопки инфо
        info1 = root.findViewById(R.id.info1);
        info1.setOnClickListener(info);
        info2 = root.findViewById(R.id.info2);
        info2.setOnClickListener(info);
        info3 = root.findViewById(R.id.info3);
        info3.setOnClickListener(info);

        //заголовки + разделители
        habitLabel1 = root.findViewById(R.id.habitLabel1);
        registerForContextMenu(habitLabel1);
        habitLabel2 = root.findViewById(R.id.habitLabel2);
        registerForContextMenu(habitLabel2);
        habitLabel3 = root.findViewById(R.id.habitLabel3);
        registerForContextMenu(habitLabel3);
        habitSep1 = root.findViewById(R.id.habitSep1);
        habitSep2 = root.findViewById(R.id.habitSep2);
        habitSep3 = root.findViewById(R.id.habitSep3);

        //меняем заголовок если Ж
        titleHome = root.findViewById(R.id.titleHome);
        sPref = requireContext().getSharedPreferences("sPref", Context.MODE_PRIVATE);
        if (sPref.getString(APP_PREFERENCES_GENDER, "").equals("FEMALE")) {
            titleHome.setText(R.string.title_habits_w);
        }

        //проверка, есть ли пропуски, добавляем провалы и дату

        //получаем сегодняшнюю дату
        Date currentDate = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        //сравниваем
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("habits", null, null, null, null, null, null);
        c.moveToFirst();

        final int lastSucIndex = c.getColumnIndex("lastSuc");
        final int failsIndex = c.getColumnIndex("fails");
        final int lastFailIndex = c.getColumnIndex("lastFail");
        final int idIndex = c.getColumnIndex("id");

        for (int i = 0; i < c.getCount(); i++) {
            if (formatForDateNow.format(new Date (System.currentTimeMillis() - 86400000)).equals(c.getString(lastFailIndex)))
                break;
            long diff = Math.abs((currentDate.getTime() / 86400000) - (c.getLong(lastSucIndex) / 86400000));
            if (diff > 1) {
                ContentValues cv = new ContentValues();
                cv.put("fails", c.getInt(failsIndex) + (diff - ((formatForDateNow.format(new Date(c.getLong(lastSucIndex))).equals(c.getString(lastFailIndex))) ? 2 : 1)));
                cv.put("lastFail", formatForDateNow.format(new Date(System.currentTimeMillis() - 86400000)));
                cv.put("lastSuc", System.currentTimeMillis() - 86400000);
                db.update("habits", cv, "id = ?", new String[] {String.valueOf(c.getInt(idIndex))});
            }
            c.moveToNext();
        }
        c.close();
        db.close();

        //показываем привычки
        habitLoad(false);

        return root;
    }

    //при возвращении из другой активности обновляем привычки
    @Override
    public void onResume() {
        super.onResume();
        habitLoad(false);

        titleHome.setText(R.string.title_habits_m);
        sPref = requireContext().getSharedPreferences("sPref", Context.MODE_PRIVATE);
        if (sPref.getString(APP_PREFERENCES_GENDER, "").equals("FEMALE")) {
            titleHome.setText(R.string.title_habits_w);
        }
    }

    //при возвращении из HabitAdd показываем снекбар
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && resultCode != Activity.RESULT_OK && resultCode != 2) return;
        String text;
        if (resultCode == Activity.RESULT_OK)
            text = "Привычка добавлена";
        else if (resultCode == 2)
            text = "Пустое название привычки";
        else text = "Достигнуто макисмальное количество привычек";
        Snackbar.make(root, text, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.nav_view)
                .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                .setTextColor(getResources().getColor(R.color.colorContrast))
                .show();
    }

    //загружаем привычки
    private void habitLoad (boolean appear) {
        SharedPreferences.Editor editor2 = achPref.edit();
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.load);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("habits", null, null, null, null, null, null);
        c.moveToFirst();
        layout1.setBackgroundColor(0x00FFFFFF);
        layout2.setBackgroundColor(0x00FFFFFF);
        layout3.setBackgroundColor(0x00FFFFFF);

        clearHabitInFirebase(); //очищаем Firebase

        for (int i = 0; i < c.getCount(); i++) {
            int createdIndex = c.getColumnIndex("created");
            int failsIndex = c.getColumnIndex("fails");
            int labelIndex = c.getColumnIndex("label");
            int lastFailIndex = c.getColumnIndex("lastFail");
            switch (i + 1) {
                case 1:
                    habitProgress(layout1, c.getInt(failsIndex), c.getInt(createdIndex));
                    habitLabel1.setText(c.getString(labelIndex));
                    if (appear) {
                        habitLabel1.startAnimation(anim);
                        info1.startAnimation(anim);
                        habitSep1.startAnimation(anim);
                    }
                    habitLabel1.setVisibility(View.VISIBLE);
                    info1.setVisibility(View.VISIBLE);
                    habitSep1.setVisibility(View.VISIBLE);

                    //добавляем в Firebase
                    addHabitInFirebase("habitOne", c.getString(labelIndex), c.getInt(createdIndex), c.getInt(failsIndex), c.getString(lastFailIndex));

                    //получение ачивки Новичок
                    if (!achPref.getBoolean("Новичок", false)) {
                        sendNotificationAchievement(1);
                        editor2.putBoolean("Новичок", true);
                        addAchievementInFirebase("Новичок");
                    }

                    break;
                case 2:
                    habitProgress(layout2, c.getInt(failsIndex), c.getInt(createdIndex));
                    habitLabel2.setText(c.getString(labelIndex));
                    if (appear) {
                        habitLabel2.startAnimation(anim);
                        info2.startAnimation(anim);
                        habitSep2.startAnimation(anim);
                    }
                    habitLabel2.setVisibility(View.VISIBLE);
                    info2.setVisibility(View.VISIBLE);
                    habitSep2.setVisibility(View.VISIBLE);

                    //добавляем в Firebase
                    addHabitInFirebase("habitTwo", c.getString(labelIndex), c.getInt(createdIndex), c.getInt(failsIndex), c.getString(lastFailIndex));

                    break;
                case 3:
                    habitProgress(layout3, c.getInt(failsIndex), c.getInt(createdIndex));
                    habitLabel3.setText(c.getString(labelIndex));
                    if (appear) {
                        habitLabel3.startAnimation(anim);
                        info3.startAnimation(anim);
                        habitSep3.startAnimation(anim);
                    }
                    habitLabel3.setVisibility(View.VISIBLE);
                    info3.setVisibility(View.VISIBLE);
                    habitSep3.setVisibility(View.VISIBLE);

                    //добавляем в Firebase
                    addHabitInFirebase("habitThree", c.getString(labelIndex), c.getInt(createdIndex), c.getInt(failsIndex), c.getString(lastFailIndex));

                    //получение привычки Бывалй
                    if (!achPref.getBoolean("Бывалый", false)) {
                        sendNotificationAchievement(2);
                        editor2.putBoolean("Бывалый", true);
                        addAchievementInFirebase("Бывалый");
                    }
                    break;
            }
            c.moveToNext();
        }
        c.close();
        db.close();
        editor2.apply();
    }

    //прогресс привычки
    private void habitProgress(LinearLayout layout, int fails, int created) {
        int BAD = 0x33FF0000, WELL = 0x33FFFF00, GOOD = 0x3300FF00;
        long days = (System.currentTimeMillis() / 1000 - created) / 60 / 60 / 24;
        double progress = ((double) fails)/((double) days);
        if (fails > days)
            layout.setBackgroundColor(BAD);
        else if (progress < 0.2)
            layout.setBackgroundColor(GOOD);
        else if (progress < 0.8)
            layout.setBackgroundColor(WELL);
        else if (progress < 1 || fails == days && fails != 0)
            layout.setBackgroundColor(BAD);
    }

    //открываем активность добавления
    private View.OnClickListener adding = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), HabitAdd.class);
            startActivityForResult(intent, 1);
        }
    };

    //открываем активность инфо
    private View.OnClickListener info = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //открыаем бд и курсор
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.query("habits", null, null, null, null, null, null);

            //создаём бандл для передачи инфы в активность инфо
            Bundle bundleLabel = new Bundle();
            Bundle bundleDays = new Bundle();
            Bundle bundleId = new Bundle();
            Bundle bundleFails = new Bundle();
            Bundle bundleLastFail = new Bundle();
            Bundle bundleLastSuc = new Bundle();

            //получаем индексы колонок
            int createdIndex = c.getColumnIndex("created");
            int idIndex = c.getColumnIndex("id");
            int failsIndex = c.getColumnIndex("fails");
            int lastFailIndex = c.getColumnIndex("lastFail");
            int lastSucIndex = c.getColumnIndex("lastSuc");

            //двигаем курсор (и заодно сохраняем названия в бандл)
            switch (v.getId()) {
                case R.id.info1:
                    c.moveToPosition(0);
                    bundleLabel.putString("Label",habitLabel1.getText().toString());
                    break;
                case R.id.info2:
                    c.moveToPosition(1);
                    bundleLabel.putString("Label",habitLabel2.getText().toString());
                    break;
                case R.id.info3:
                    c.moveToPosition(2);
                    bundleLabel.putString("Label",habitLabel3.getText().toString());
                    break;
            }

            //сохраняем в бандл остальную инфу из бд
            bundleDays.putString("Days",c.getString(createdIndex));
            bundleId.putInt("Id",c.getInt(idIndex));
            bundleFails.putInt("Fails", c.getInt(failsIndex));
            bundleLastFail.putString("LastFail", c.getString(lastFailIndex));
            bundleLastSuc.putLong("LastSuc", c.getLong(lastSucIndex));

            //закрываем курсор и бд
            c.close();
            db.close();

            //открываем активность инфо и передаём данные
            Intent intent = new Intent(getActivity(), HabitInfo.class);
            intent.putExtras(bundleLabel);
            intent.putExtras(bundleDays);
            intent.putExtras(bundleId);
            intent.putExtras(bundleFails);
            intent.putExtras(bundleLastFail);
            intent.putExtras(bundleLastSuc);
            startActivity(intent);
        }
    };

    //контестное меню для удаления привычки (одной)
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
            case R.id.habitLabel1:
                menu.add(0, 1, 0, "Удалить");
                break;
            case R.id.habitLabel2:
                menu.add(0, 2, 0, "Удалить");
                break;
            case R.id.habitLabel3:
                menu.add(0, 3, 0, "Удалить");
                break;
        }
    }

    //удаление привычки (одной)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("habits", null, null, null, null, null, null);
        int previous = c.getCount();
        c.moveToPosition(item.getItemId() - 1);
        db.delete("habits", "id = " + c.getInt(0), null);
        c.close();
        db.close();
        habitUpdate(previous);

        //показываем снекбар
        Snackbar.make(root, "Привычка удалена", Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.nav_view)
                .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                .setTextColor(getResources().getColor(R.color.colorContrast))
                .show();
        return super.onContextItemSelected(item);
    }

    //обновление экрана привычек при удалении привычки
    private void habitUpdate(int previous) {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.vanish);
        switch (previous) {
            case 3:
            habitLabel3.startAnimation(anim);
            info3.startAnimation(anim);
            habitSep3.startAnimation(anim);
            case 2:
            habitLabel2.startAnimation(anim);
            info2.startAnimation(anim);
            habitSep2.startAnimation(anim);
            case 1:
            habitLabel1.startAnimation(anim);
            info1.startAnimation(anim);
            habitSep1.startAnimation(anim);
        }
        Handler vanishHandler = new Handler();
        Runnable vanishRunnable = new Runnable() {
            @Override
            public void run() {
                habitLabel1.setVisibility(View.INVISIBLE);
                info1.setVisibility(View.INVISIBLE);
                habitSep1.setVisibility(View.INVISIBLE);
                habitLabel2.setVisibility(View.INVISIBLE);
                info2.setVisibility(View.INVISIBLE);
                habitSep2.setVisibility(View.INVISIBLE);
                habitLabel3.setVisibility(View.INVISIBLE);
                info3.setVisibility(View.INVISIBLE);
                habitSep3.setVisibility(View.INVISIBLE);
                habitLoad(true);
            }
        };
        vanishHandler.postDelayed(vanishRunnable, 490);
    }

    //отправление уведомления о новом достижении
    private void sendNotificationAchievement(int achievementId) {
        Intent resultIntent = new Intent(getActivity(), AchievementsFragment.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("achievements", "Достижения", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.createNotificationChannel(notificationChannel);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "achievements")
                    .setSmallIcon(R.drawable.ic_achievements_24dp)
                    .setContentTitle("Новое достижение")
                    .setContentText("Откройте вкладку «Достижения», чтобы посмотреть")
                    .setAutoCancel(true)
                    .setContentIntent(resultPendingIntent);
            Notification notification = builder.build();
            notificationManager.notify(achievementId, notification);
        }
        else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext())
                    .setSmallIcon(R.drawable.ic_achievements_24dp)
                    .setContentTitle("Новое достижение")
                    .setContentText("Откройте вкладку «Достижения», чтобы посмотреть")
                    .setAutoCancel(true)
                    .setContentIntent(resultPendingIntent);
            Notification notification = builder.build();
            notificationManager.notify(achievementId, notification);
        }
    }

    //добавляем привычку в онлайн БД
    private void addHabitInFirebase (String habitNum, String label, int created, int fails, String lastFail) {
        database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        database.child("habits").child(habitNum).child("label").setValue(label);
        database.child("habits").child(habitNum).child("created").setValue(created);
        database.child("habits").child(habitNum).child("fails").setValue(fails);
        database.child("habits").child(habitNum).child("lastFail").setValue(lastFail);
    }

    //удаляем привычку в онлайн БД
    private void clearHabitInFirebase() {
        database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        database.child("habits").child("habitOne").removeValue();
        database.child("habits").child("habitTwo").removeValue();
        database.child("habits").child("habitThree").removeValue();
    }

    //добавляем ачивку в онлайн БД
    private void addAchievementInFirebase(String achievement) {
        database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        database.child("achievements").child(achievement).setValue(true);
    }
}
