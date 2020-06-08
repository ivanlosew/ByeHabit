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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.byehabit.Callback;
import com.example.byehabit.R;
import com.example.byehabit.TagForIncomingRequests;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendsIncomingRequests extends AppCompatActivity {

    private LinearLayout layout;
    private TextView errorMessage;
    private ProgressBar loading;
    private DatabaseReference incomingDatabase;
    private SharedPreferences nicknamePref;
    private View root;

    private final int wrapContent = LinearLayout.LayoutParams.WRAP_CONTENT;
    private final int matchParent = LinearLayout.LayoutParams.MATCH_PARENT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_incoming_requests);

        root = findViewById(R.id.root);

        nicknamePref = getApplicationContext().getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);

        layout = findViewById(R.id.layout);
        layout.removeAllViews();

        //анимация загрузки + ошибка, если нет инета
        errorMessage = findViewById(R.id.errorMessage);
        loading = findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE); //анимация загрузки
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loading.getVisibility() == View.VISIBLE) {
                    errorMessage.setText("Нет соединения с интернетом");
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_from_top);
                    errorMessage.startAnimation(anim);
                    errorMessage.setVisibility(View.VISIBLE);
                    stopLoadingAnim();
                }
            }
        }, 5000);

        incomingDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("incoming");
        incomingDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //создаём список со всеми входящими заявками
                if (dataSnapshot.getValue() == null) {
                    return;
                }
                final List<String> incomingList = new ArrayList<>();
                String incomingRequests = dataSnapshot.getValue().toString();
                while (incomingRequests.contains(",")) {
                    incomingList.add(incomingRequests.substring(0, incomingRequests.indexOf(",")));
                    incomingRequests = incomingRequests.substring(incomingRequests.indexOf(",") + 1);
                }
                incomingList.add(incomingRequests);

                //параметры лэйаутов лэйаута, текста и кнопок
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
                final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(matchParent, wrapContent);
                final LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(matchParent, matchParent);

                titleParams.weight = 1;
                titleParams.setMargins(0, 8, 0, 8);
                titleParams.gravity = Gravity.CENTER_VERTICAL;

                buttonParams.weight = 3;
                buttonParams.gravity = Gravity.CENTER_VERTICAL;

                //показываем все входящие заявки
                for (int i = 0; i < incomingList.size(); i++) {
                    final int finalI = i;
                    getNicknameById(incomingList.get(i), new Callback() {
                        @Override
                        public void onCallback(String value) {
                            LinearLayout newLayout = new LinearLayout(getApplicationContext());
                            newLayout.setOrientation(LinearLayout.HORIZONTAL);
                            layout.addView(newLayout, layoutParams);

                            TextView textView = new TextView(getApplicationContext());
                            textView.setText(value);
                            textView.setTextSize(36);
                            textView.setTypeface(Typeface.DEFAULT_BOLD);
                            textView.setTextColor(getResources().getColor(R.color.colorAccent));
                            newLayout.addView(textView, titleParams);

                            TagForIncomingRequests tag = new TagForIncomingRequests(incomingList.get(finalI), newLayout);

                            ImageButton buttonAccept = new ImageButton(getApplicationContext());
                            buttonAccept.setImageResource(R.drawable.ic_friends_accept);
                            buttonAccept.setColorFilter(getResources().getColor(R.color.colorPrimary));
                            buttonAccept.setBackgroundColor(0x00FFFFFF);
                            buttonAccept.setTag(tag);
                            buttonAccept.setOnClickListener(accept);
                            newLayout.addView(buttonAccept, buttonParams);

                            ImageButton buttonDecline = new ImageButton(getApplicationContext());
                            buttonDecline.setImageResource(R.drawable.ic_friends_decline);
                            buttonDecline.setColorFilter(getResources().getColor(R.color.colorPrimary));
                            buttonDecline.setBackgroundColor(0x00FFFFFF);
                            buttonDecline.setTag(tag);
                            buttonDecline.setOnClickListener(decline);
                            newLayout.addView(buttonDecline, buttonParams);
                        }
                    });
                }
                //убираем анимацию загрузки
                stopLoadingAnim();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //получаем никнейм по айди
    private void getNicknameById (String id, final Callback callback) {
        incomingDatabase = FirebaseDatabase.getInstance().getReference(id).child("nickname");
        incomingDatabase.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String nicknameById = dataSnapshot.getValue(String.class);
                        callback.onCallback(nicknameById);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //errorMessage.setText("Неизвестная ошибка");
                        //errorMessage.setVisibility(View.VISIBLE);
                    }
                });
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

    //принимаем заявку
    View.OnClickListener accept = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            incomingDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("incoming");
            incomingDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final TagForIncomingRequests tag = (TagForIncomingRequests) v.getTag();
                    final String id = tag.getIdOfRequest();
                    if (!dataSnapshot.getValue().toString().contains(","))
                        incomingDatabase.removeValue();
                    else {
                        String newString = dataSnapshot.getValue().toString();
                        if (newString.contains(id.toString() + ","))
                            newString = newString.replace(id + ",", "");
                        else if (newString.contains("," + id))
                            newString = newString.replace("," + id, "");
                        incomingDatabase.setValue(newString);
                    }
                    final DatabaseReference friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("friends");
                    friendsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            String newString;
                            if (dataSnapshot2.getValue() != null) {
                                newString = dataSnapshot2.getValue().toString();
                                newString += "," + id;
                            }
                            else newString = id;
                            friendsDatabase.setValue(newString);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    final DatabaseReference secondFriendsDatabase = FirebaseDatabase.getInstance().getReference(id).child("friends");
                    secondFriendsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            String newString;
                            if (dataSnapshot2.getValue() != null) {
                                newString = dataSnapshot2.getValue().toString();
                                newString += "," + nicknamePref.getString("id", "0");
                            }
                            else newString = nicknamePref.getString("id", "0");
                            secondFriendsDatabase.setValue(newString);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    final DatabaseReference outcomingDatabase = FirebaseDatabase.getInstance().getReference(id).child("outcoming");
                    outcomingDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            if (!dataSnapshot2.getValue().toString().contains(","))
                                outcomingDatabase.removeValue();
                            else {
                                String newString = dataSnapshot2.getValue().toString();
                                if (newString.contains(id + ","))
                                    newString = newString.replace(id + ",", "");
                                else if (newString.contains("," + id))
                                    newString = newString.replace("," + id, "");
                                outcomingDatabase.setValue(newString);
                            }

                            //убираем эту заявку
                            LinearLayout newLayout = tag.getLayoutOfRequest();
                            newLayout.setVisibility(View.GONE);

                            //показываем снекбар
                            Snackbar.make(root, "Заявка принята", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                                    .setTextColor(getResources().getColor(R.color.colorContrast))
                                    .show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    };

    //отклоняем заявку
    View.OnClickListener decline = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            incomingDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("incoming");
            incomingDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final TagForIncomingRequests tag = (TagForIncomingRequests) v.getTag();
                    final String id = tag.getIdOfRequest();
                    if (!dataSnapshot.getValue().toString().contains(","))
                        incomingDatabase.removeValue();
                    else {
                        String newString = dataSnapshot.getValue().toString();
                        if (newString.contains(id.toString() + ","))
                            newString = newString.replace(id + ",", "");
                        else if (newString.contains("," + id))
                            newString = newString.replace("," + id, "");
                        incomingDatabase.setValue(newString);
                    }
                    final DatabaseReference outcomingDatabase = FirebaseDatabase.getInstance().getReference(id).child("outcoming");
                    outcomingDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            if (!dataSnapshot2.getValue().toString().contains(","))
                                outcomingDatabase.removeValue();
                            else {
                                String newString = dataSnapshot2.getValue().toString();
                                if (newString.contains(id + ","))
                                    newString = newString.replace(id + ",", "");
                                else if (newString.contains("," + id))
                                    newString = newString.replace("," + id, "");
                                outcomingDatabase.setValue(newString);
                            }

                            //убираем эту заявку
                            LinearLayout newLayout = tag.getLayoutOfRequest();
                            newLayout.setVisibility(View.GONE);

                            //показываем снекбар
                            Snackbar.make(root, "Заявка отклонена", Snackbar.LENGTH_LONG)
                                    .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                                    .setTextColor(getResources().getColor(R.color.colorContrast))
                                    .show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    };
}
