package com.example.byehabit.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.byehabit.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private RadioButton appearanceLight, appearanceDark, appearanceAuto;
    private RadioButton male, female;
    private EditText newNickname;
    private Button changeNickname;
    private SharedPreferences sPref;
    private TextView errorMessage;
    public static final String APP_PREFERENCES_MODE = "MODE";
    public static final String APP_PREFERENCES_GENDER = "GENDER";

    //загрузка, успех и ошибка
    private ProgressBar loading;
    private ImageView result;

    //никнейм
    private SharedPreferences nicknamePref;
    private DatabaseReference database;
    private ArrayList<String> allNicknames; //все никнеймы

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sPref = getSharedPreferences("sPref", Context.MODE_PRIVATE);
        nicknamePref = getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);

        //изменение никнейма
        newNickname = findViewById(R.id.newNickname);
        changeNickname = findViewById(R.id.changeNickname);
        newNickname.setText(nicknamePref.getString("nickname", ""));
        changeNickname.setOnClickListener(nickname);
        allNicknames = new ArrayList<>();
        errorMessage = findViewById(R.id.errorMessage);

        //кнопки для изменения темы
        appearanceLight = findViewById(R.id.appearanceLight);
        appearanceDark = findViewById(R.id.appearanceDark);
        appearanceAuto = findViewById(R.id.appearanceAuto);
        appearanceLight.setOnClickListener(mode);
        appearanceDark.setOnClickListener(mode);
        appearanceAuto.setOnClickListener(mode);

        //ставим выбранную кнопку
        String MODE = sPref.getString(APP_PREFERENCES_MODE, "");
        switch (MODE) {
            case "LIGHT":
                appearanceLight.setChecked(true);
                break;
            case "DARK":
                appearanceDark.setChecked(true);
                break;
            case "AUTO":
            case "BATTERY":
                appearanceAuto.setChecked(true);
                break;
        }

        //кнопки выбора пола
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);
        male.setOnClickListener(gender);
        female.setOnClickListener(gender);

        //ставим выбранную кнопку
        String GENDER = sPref.getString(APP_PREFERENCES_GENDER, "MALE");
        switch (GENDER) {
            case "MALE":
                male.setChecked(true);
                break;
            case "FEMALE":
                female.setChecked(true);
                break;
        }

        //изменение ника: загрузка, успех, ошибка
        loading = findViewById(R.id.loading);
        result = findViewById(R.id.result);
    }

    //меняем тему
    View.OnClickListener mode = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPreferences.Editor editor = sPref.edit();
            switch (v.getId()) {
                case R.id.appearanceLight:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor.putString(APP_PREFERENCES_MODE, "LIGHT");
                    break;
                case R.id.appearanceDark:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor.putString(APP_PREFERENCES_MODE, "DARK");
                    break;
                case R.id.appearanceAuto:
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        editor.putString(APP_PREFERENCES_MODE, "AUTO");
                    }
                    else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                        editor.putString(APP_PREFERENCES_MODE, "BATTERY");
                    }
                    break;
            }
            editor.apply();
        }
    };

    //меняем пол
    View.OnClickListener gender = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPreferences.Editor editor = sPref.edit();
            database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
            switch (v.getId()) {
                case R.id.male:
                    editor.putString(APP_PREFERENCES_GENDER, "MALE");
                    female.setChecked(false);
                    database.child("gender").setValue("M"); //добавляем в Firebase
                    break;
                case R.id.female:
                    editor.putString(APP_PREFERENCES_GENDER, "FEMALE");
                    male.setChecked(false);
                    database.child("gender").setValue("F"); //добавляем в Firebase
                    break;
            }
            editor.apply();
        }
    };

    //меняем ник
    View.OnClickListener nickname = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (nicknamePref.getString("nickname", "").equals(newNickname.getText().toString()))
                return;
            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_right);
            loading.startAnimation(anim);
            loading.setVisibility(View.VISIBLE);
            database = FirebaseDatabase.getInstance().getReference();
            database.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            allNicknames = collectNicknames((Map<String, Object>) dataSnapshot.getValue());
                            if (allNicknames.contains(newNickname.getText().toString())) {
                                errorMessage.setText("Никнейм уже занят");
                                errorMessage.setVisibility(View.VISIBLE);
                                errorChange();
                                return;
                            }
                            if (!hasConnection(getApplicationContext()) || allNicknames.isEmpty()) {
                                errorMessage.setText("Отсутствует подключение к интернету");
                                errorMessage.setVisibility(View.VISIBLE);
                                errorChange();
                                return;
                            }
                            if (!checkNickname(newNickname.getText().toString())) {
                                errorChange();
                                return;
                            }

                            SharedPreferences.Editor editor = nicknamePref.edit();
                            newNickname.setText(newNickname.getText().toString().trim().replaceAll(" +", ""));
                            editor.putString("nickname", newNickname.getText().toString());
                            editor.apply();
                            errorMessage.setVisibility(View.GONE);
                            database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
                            database.child("nickname").setValue(nicknamePref.getString("nickname", null));
                            successfulChange();

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            errorMessage.setText("Неизвестная ошибка");
                            errorMessage.setVisibility(View.VISIBLE);
                        }
                    });
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (loading.getVisibility() == View.VISIBLE) {
                        errorChange();
                        errorMessage.setText("Нет соединения с интернетом");
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                }
            }, 5000);
        }
    };

    //получаем все никнеймы (чтобы проверять, не занят ли)
    private ArrayList<String> collectNicknames(Map<String,Object> users) {
        ArrayList<String> nicknames = new ArrayList<>();
        for (Map.Entry<String, Object> entry : users.entrySet()){
            Map singleUser = (Map) entry.getValue();
            nicknames.add((String) singleUser.get("nickname"));
        }
        return nicknames;
    }

    //если успешно изменилось, показываем галочку и прячем
    private void successfulChange() {
        result.setImageResource(R.drawable.ic_tick_animated);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loading.setVisibility(View.GONE);
                result.setVisibility(View.VISIBLE);
                ((Animatable) result.getDrawable()).start();
                Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.remove_to_right);
                        result.startAnimation(anim);
                        Handler handler2 = new Handler();
                        handler2.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                result.setVisibility(View.GONE);
                            }
                        }, 190);
                    }
                }, 500);
            }
        }, 300);
    }

    //если ошибка, показываем анимацию
    private void errorChange() {
        result.setImageResource(R.drawable.ic_cross_animated);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            loading.setVisibility(View.GONE);
            result.setVisibility(View.VISIBLE);
            ((Animatable) result.getDrawable()).start();
            Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.remove_to_right);
                    result.startAnimation(anim);
                    Handler handler2 = new Handler();
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            result.setVisibility(View.GONE);
                        }
                    }, 190);
                }
            }, 300);
        }
    }, 300);
}

//проверяем никнейм
    private boolean checkNickname(String nickname) {
        if (nickname.contains("#") || nickname.contains("*") || nickname.contains("/") || nickname.contains("&") || nickname.contains(":") || nickname.contains(";") || nickname.contains("%") || nickname.contains("@") || nickname.contains("?") || nickname.contains("!") || nickname.contains("+") || nickname.contains("[") || nickname.contains("]") || nickname.contains("(") || nickname.contains(")")) {
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText("Никнейм не должен содержать специальных символов");
            return false;
        }
        else if (nickname.length() <= 3) {
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText("Никнейм слишком короткий");
            return false;
        }
        else if (nickname.length() >= 15) {
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText("Никнейм слишком длинный");
            return false;
        }
        else return true;
    }

    //проверяем, есть ли интернет (чтобы ускорить процесс)
    private boolean hasConnection(final Context context)
    {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected())
        {
            return true;
        }
        return false;
    }
}
