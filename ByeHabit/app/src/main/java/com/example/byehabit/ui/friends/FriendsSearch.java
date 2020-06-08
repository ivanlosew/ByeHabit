package com.example.byehabit.ui.friends;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.byehabit.Callback;
import com.example.byehabit.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FriendsSearch extends AppCompatActivity {

    private EditText searchField;
    private ImageButton searchButton;
    private TextView errorMessage;
    private LinearLayout layout;
    private ProgressBar loading;
    private DatabaseReference database;
    private SharedPreferences nicknamePref;
    private List<String> allNicknames;
    private List<String> friendsList;

    private final int wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT;
    private final int matchParent = LinearLayout.LayoutParams.MATCH_PARENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_search);

        nicknamePref = getApplicationContext().getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);

        searchField = findViewById(R.id.searchField);
        searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(search);
        errorMessage = findViewById(R.id.errorMessage);
        layout = findViewById(R.id.layout);
        loading = findViewById(R.id.loading);

        //список друзей
        Bundle friendsBundle;
        friendsBundle = this.getIntent().getExtras();
        assert friendsBundle != null;
        friendsList = friendsBundle.getStringArrayList("friendsList");
    }

    //выполняем поиск
    View.OnClickListener search = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //если пусто
            if (searchField.getText().toString().isEmpty()) {
                errorMessage.setText("Пустой запрос");
                Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_top);
                errorMessage.startAnimation(anim);
                errorMessage.setVisibility(View.VISIBLE);
                stopLoadingAnim();
                return;
            }

            layout.removeAllViews(); //удаляем предыдущие результаты

            loading.setVisibility(View.VISIBLE); //анимация загрузки

            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
            final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
            final LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(matchParent, matchParent);

            titleParams.weight = 1;
            titleParams.setMargins(0, 8, 0, 8);
            titleParams.gravity = Gravity.CENTER_VERTICAL;

            buttonParams.weight = 6;
            buttonParams.gravity = Gravity.CENTER_VERTICAL;

            database = FirebaseDatabase.getInstance().getReference();
            database.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            allNicknames = collectNicknames((Map<String, Object>) dataSnapshot.getValue());
                            List<String> suitableNicknames = new ArrayList<>();


                            //ищем только подходящие никнеймы
                            for (int i = 0; i < allNicknames.size(); i++) {
                                if (allNicknames.get(i).contains(searchField.getText().toString()))
                                    if (!allNicknames.get(i).equals(nicknamePref.getString("nickname", "0")))
                                            suitableNicknames.add(allNicknames.get(i));
                            }

                            //если ничего не найдено
                            if (suitableNicknames.size() == 0) {
                                if (errorMessage.getVisibility() == View.VISIBLE && errorMessage.getText().equals("Ничего не найдено")) {
                                    stopLoadingAnim();
                                    return;
                                }
                                errorMessage.setText("Ничего не найдено");
                                Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_top);
                                errorMessage.startAnimation(anim);
                                errorMessage.setVisibility(View.VISIBLE);
                                stopLoadingAnim();
                                return;
                            }
                            //прячем сообщение об ошибке
                            if (errorMessage.getVisibility() == View.VISIBLE) {
                                Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.remove_to_top);
                                errorMessage.startAnimation(anim);
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        errorMessage.setVisibility(View.GONE);
                                    }
                                }, 200);
                            }
                            //показываем результаты поиска
                            for (int i = 0; i < suitableNicknames.size(); i++) {
                                LinearLayout newLayout = new LinearLayout(getApplicationContext());
                                newLayout.setOrientation(LinearLayout.HORIZONTAL);
                                layout.addView(newLayout, layoutParams);

                                TextView textView = new TextView(getApplicationContext());
                                textView.setText(suitableNicknames.get(i));
                                textView.setTextSize(36);
                                textView.setTypeface(Typeface.DEFAULT_BOLD);
                                textView.setTextColor(getResources().getColor(R.color.colorAccent));
                                newLayout.addView(textView, titleParams);

                                ImageButton imageButton = new ImageButton(getApplicationContext());
                                imageButton.setImageResource(R.drawable.ic_friends_accept);
                                imageButton.setColorFilter(getResources().getColor(R.color.colorPrimary));
                                imageButton.setBackgroundColor(0x00FFFFFF);
                                imageButton.setTag(suitableNicknames.get(i));
                                imageButton.setOnClickListener(addFriend);
                                newLayout.addView(imageButton, buttonParams);

                            }
                            stopLoadingAnim();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    };

    //получаем все никнеймы (убираем из результатов тех, кто уже в друзьях)
    private ArrayList<String> collectNicknames(Map<String,Object> users) {
        ArrayList<String> nicknames = new ArrayList<>();
        for (Map.Entry<String, Object> entry : users.entrySet()){
            boolean suitable = true;
            for (int i = 0; i < friendsList.size(); i++) {
                if (entry.getKey().equals(friendsList.get(i)))
                    suitable = false;
            }
            if (suitable) {
                Map singleUser = (Map) entry.getValue();
                if (singleUser.get("nickname") != null)
                nicknames.add((String) singleUser.get("nickname"));
            }
        }
        return nicknames;
    }

    //останавливаем анимацию загрузки
    private void stopLoadingAnim () {
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
    }

    //запрос в друзья
    View.OnClickListener addFriend = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("outcoming");
            database.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                            getIdByNickname(v.getTag().toString(), new Callback() {
                                @Override
                                public void onCallback(String value) {
                                    database = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("outcoming");
                                    //если пусто, просто добавляем
                                    if (dataSnapshot.getValue() == null)
                                        database.setValue(value);
                                    //проверяем, не подавалась ли уже заявка
                                    else if (dataSnapshot.getValue().toString().contains(value)) {
                                        errorMessage.setText("Заявка отправлена");
                                        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_top);
                                        errorMessage.startAnimation(anim);
                                        errorMessage.setVisibility(View.VISIBLE);
                                        return;
                                    }
                                    //если не пусто, то добавляем к имеющемуся
                                    else {
                                        String newString = dataSnapshot.getValue().toString() + "," + value;
                                        database.setValue(newString);
                                    }
                                    final String requestId = value;
                                    database = FirebaseDatabase.getInstance().getReference(value).child("incoming");
                                    database.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            DatabaseReference newDatabase = FirebaseDatabase.getInstance().getReference(requestId).child("incoming");
                                            if (dataSnapshot.getValue() == null) {
                                                newDatabase.setValue(nicknamePref.getString("id", "0"));
                                            }
                                            else if (dataSnapshot.getValue().toString().contains(nicknamePref.getString("id", "0")))
                                                return;
                                            else {
                                                String newIncoming = dataSnapshot.getValue().toString();
                                                newIncoming += "," + nicknamePref.getString("id", "0");
                                                newDatabase.setValue(newIncoming);
                                            }
                                            v.setVisibility(View.INVISIBLE);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            errorMessage.setText("Неизвестная ошибка");
                                            errorMessage.setVisibility(View.VISIBLE);
                                        }
                                    });
                                    errorMessage.setText("Заявка отправлена");
                                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_top);
                                    errorMessage.startAnimation(anim);
                                    errorMessage.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            errorMessage.setText("Неизвестная ошибка");
                            errorMessage.setVisibility(View.VISIBLE);
                        }
                    });
        }
    };

    //получаем айди по никнейму
    private void getIdByNickname(String nickname, final Callback callback) {
        database = FirebaseDatabase.getInstance().getReference();
        Query query = database.orderByChild("nickname").equalTo(nickname);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String id = Objects.requireNonNull(dataSnapshot.getValue()).toString();
                id = id.substring(1, id.indexOf("="));
                callback.onCallback(id);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                errorMessage.setText("Неизвестная ошибка");
                errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }
}
