package com.example.byehabit.ui.friends;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.byehabit.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class FriendsAchievements extends AppCompatActivity {

    private ImageView achievementImage1, achievementImage2, achievementImage3;
    private TextView empty, achievementTitle1, achievementTitle2, achievementTitle3;
    private ProgressBar loading;

    public static final int NOVICHOK = 1, BYVALYI = 2, DESYATKA = 3;
    private boolean[] isSet = new boolean[4];

    private DatabaseReference friendAchievementsDatabase;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_achievements);

        empty = findViewById(R.id.empty);

        bundle = this.getIntent().getExtras();

        achievementImage1 = findViewById(R.id.achievementImage1);
        achievementImage2 = findViewById(R.id.achievementImage2);
        achievementImage3 = findViewById(R.id.achievementImage3);
        achievementTitle1 = findViewById(R.id.achievementTitle1);
        achievementTitle2 = findViewById(R.id.achievementTitle2);
        achievementTitle3 = findViewById(R.id.achievementTitle3);

        loading = findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE); //анимация загрузки

        isSet[1] = false; isSet[2] = false; isSet[3] = false;

        //меняем статус бар
        Objects.requireNonNull(getSupportActionBar()).setTitle("Достижения " + bundle.getString("nickname"));

        //показываем достижения
        loadAchievements();
    }

    //показываем достижения
    private void loadAchievements () {
        friendAchievementsDatabase = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(bundle.getString("id"))).child("achievements");
        friendAchievementsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (int i = 1; i <= 3; i++) {
                    if (Objects.equals(dataSnapshot.child("Новичок").getValue(), true) && !isSet[NOVICHOK]) {
                        setAchievement("Новичок", R.drawable.ach_novichok, i);
                        isSet[NOVICHOK] = true;
                    } else if (Objects.equals(dataSnapshot.child("Бывалый").getValue(), true) && !isSet[BYVALYI]) {
                        setAchievement("Бывалый", R.drawable.ach_byvaliy, i);
                        isSet[BYVALYI] = true;
                    } else if (Objects.equals(dataSnapshot.child("Десятка").getValue(), true) && !isSet[DESYATKA]) {
                        setAchievement("Десятка", R.drawable.ach_desyatka, i);
                        isSet[DESYATKA] = true;
                    }
                }
                //останавливаем анимацию
                if (loading.getVisibility() == View.VISIBLE) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.vanish);
                            loading.startAnimation(anim);
                            Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    loading.setVisibility(View.GONE);
                                }
                            }, 500);
                        }
                    }, 500);
                }

                //если пусто -- показыаем плашку
                if (!isSet[NOVICHOK] && !isSet[BYVALYI] && !isSet[DESYATKA]) {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.load);
                    empty.startAnimation(anim);
                    empty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    //вспомогательный метод (с помощью него ставится конкретное достижение)
    private void setAchievement (String achievement, int drawable, int position) {
        switch (position) {
            case 1:
                achievementImage1.setImageResource(drawable);
                achievementImage1.setVisibility(View.VISIBLE);
                achievementTitle1.setText(achievement);
                achievementTitle1.setVisibility(View.VISIBLE);
                break;
            case 2:
                achievementImage2.setImageResource(drawable);
                achievementImage2.setVisibility(View.VISIBLE);
                achievementTitle2.setText(achievement);
                achievementTitle2.setVisibility(View.VISIBLE);
                break;
            case 3:
                achievementImage3.setImageResource(drawable);
                achievementImage3.setVisibility(View.VISIBLE);
                achievementTitle3.setText(achievement);
                achievementTitle3.setVisibility(View.VISIBLE);
                break;
        }
    }
}
