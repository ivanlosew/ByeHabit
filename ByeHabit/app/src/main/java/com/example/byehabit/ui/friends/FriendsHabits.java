package com.example.byehabit.ui.friends;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.byehabit.Callback;
import com.example.byehabit.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class FriendsHabits extends AppCompatActivity {

    private TextView title, empty;
    private LinearLayout layout;
    private ProgressBar loading;

    private DatabaseReference friendHabitsDatabase;
    private DatabaseReference friendsDatabase;
    private Bundle bundle;

    private final int wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT;
    private final int matchParent = LinearLayout.LayoutParams.MATCH_PARENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_habits);

        title = findViewById(R.id.title);
        empty = findViewById(R.id.empty);
        layout = findViewById(R.id.layout);

        loading = findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE); //анимация загрузки

        bundle = this.getIntent().getExtras();

        Objects.requireNonNull(getSupportActionBar()).setTitle("Привычки " + bundle.getString("nickname"));

        //ставим никнейм в заголовок (+ меняем окончание)
        getNicknameById(bundle.getString("id"), new Callback() {
            @Override
            public void onCallback(final String value) {
                friendsDatabase = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(bundle.getString("id"))).child("gender");
                friendsDatabase.addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists())
                                    title.setText(value + " бросил...");
                                else {
                                    if (Objects.requireNonNull(dataSnapshot.getValue()).toString().equals("F"))
                                        title.setText(value + " бросила...");
                                    else title.setText(value + " бросил...");
                                }
                                Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.load);
                                title.startAnimation(anim);
                                title.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });
        loadHabits();
    }

    //показываем привычки
    private void loadHabits() {
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
        final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(wrapContent, wrapContent);
        final LinearLayout.LayoutParams daysParams = new LinearLayout.LayoutParams(wrapContent, wrapContent);

        titleParams.weight = 1;
        titleParams.setMargins(0, 8, 0, 8);
        titleParams.gravity = Gravity.CENTER_VERTICAL;

        daysParams.weight = 0;
        daysParams.gravity = Gravity.CENTER_VERTICAL;

        friendHabitsDatabase = FirebaseDatabase.getInstance().getReference(Objects.requireNonNull(bundle.getString("id"))).child("habits");
        friendHabitsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    //полоска
                    LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
                    lineParams.setMargins(0, 4, 0, 4);
                    TextView line = new TextView(getApplicationContext());
                    line.setHeight(2);
                    line.setBackground(getResources().getDrawable(R.color.colorPrimary));
                    if (i != 0)
                        layout.addView(line);

                    LinearLayout newLayout = new LinearLayout(getApplicationContext());
                    newLayout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.addView(newLayout, layoutParams);

                    String databaseChild = "";
                    switch (i) {
                        case 0:
                            databaseChild = "habitOne";
                            break;
                        case 1:
                            databaseChild = "habitTwo";
                            break;
                        case 2:
                            databaseChild = "habitThree";
                            break;
                    }

                    TextView labelView = new TextView(getApplicationContext());
                    labelView.setText(Objects.requireNonNull(dataSnapshot.child(databaseChild).child("label").getValue()).toString());
                    labelView.setTextSize(36);
                    labelView.setTextColor(getResources().getColor(R.color.colorAccent));
                    labelView.setTypeface(Typeface.DEFAULT_BOLD);
                    newLayout.addView(labelView, titleParams);

                    TextView daysView = new TextView(getApplicationContext());
                    String days = String.valueOf((System.currentTimeMillis() / 1000 - Long.parseLong(Objects.requireNonNull(dataSnapshot.child(databaseChild).child("created").getValue()).toString())) / 60 / 60 / 24);
                    days += declensionDays(Integer.parseInt(days)) + " назад";
                    daysView.setText(days);
                    daysView.setTextSize(24);
                    daysView.setTextColor(getResources().getColor(R.color.colorAccent));
                    newLayout.addView(daysView, daysParams);
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
                if (dataSnapshot.getChildrenCount() == 0) {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.load);
                    empty.startAnimation(anim);
                    empty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

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

    //получаем никнейм по айди
    private void getNicknameById (String id, final Callback callback) {
        friendsDatabase = FirebaseDatabase.getInstance().getReference(id).child("nickname");
        friendsDatabase.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String nicknameById = dataSnapshot.getValue(String.class);
                        callback.onCallback(nicknameById);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }
}
