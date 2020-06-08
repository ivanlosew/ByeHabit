package com.example.byehabit.ui.friends;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.byehabit.Callback;
import com.example.byehabit.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendsFragment extends Fragment {

    private DatabaseReference friendsDatabase;
    private SharedPreferences nicknamePref;
    private String allFriends;
    private List<String> allFriendsList;
    private Button newRequest;
    private TextView errorMessage;
    private LinearLayout layout1, layout2, layout3, layout4, layout5;
    private TextView friend1, friend2, friend3, friend4, friend5;
    private ImageButton habits1, habits2, habits3, habits4, habits5, achievements1, achievements2, achievements3, achievements4, achievements5;
    private ProgressBar loading;
    private boolean noNickname; //нет никнейма (просим установить)
    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_friends, container, false);

        noNickname = false;

        nicknamePref = requireContext().getSharedPreferences("nicknamePref", Context.MODE_PRIVATE);
        errorMessage = root.findViewById(R.id.errorMessage);

        //проверяем, есть ли никнейм
        if (nicknamePref.getString("nickname", "0").equals("0")) {
            noNickname = true;
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText("Чтобы воспользоваться этим разделом, сначала нужно добавить никнейм в настройках");
        }

        layout1 = root.findViewById(R.id.layout1);
        layout2 = root.findViewById(R.id.layout2);
        layout3 = root.findViewById(R.id.layout3);
        layout4 = root.findViewById(R.id.layout4);
        layout5 = root.findViewById(R.id.layout5);

        friend1 = root.findViewById(R.id.friend1);
        registerForContextMenu(friend1);
        friend2 = root.findViewById(R.id.friend2);
        registerForContextMenu(friend2);
        friend3 = root.findViewById(R.id.friend3);
        registerForContextMenu(friend3);
        friend4 = root.findViewById(R.id.friend4);
        registerForContextMenu(friend4);
        friend5 = root.findViewById(R.id.friend5);
        registerForContextMenu(friend5);

        habits1 = root.findViewById(R.id.habits1);
        habits1.setOnClickListener(habits);
        habits2 = root.findViewById(R.id.habits2);
        habits2.setOnClickListener(habits);
        habits3 = root.findViewById(R.id.habits3);
        habits3.setOnClickListener(habits);
        habits4 = root.findViewById(R.id.habits4);
        habits4.setOnClickListener(habits);
        habits5 = root.findViewById(R.id.habits5);
        habits5.setOnClickListener(habits);

        achievements1 = root.findViewById(R.id.achievements1);
        achievements1.setOnClickListener(achievements);
        achievements2 = root.findViewById(R.id.achievements2);
        achievements2.setOnClickListener(achievements);
        achievements3 = root.findViewById(R.id.achievements3);
        achievements3.setOnClickListener(achievements);
        achievements4 = root.findViewById(R.id.achievements4);
        achievements4.setOnClickListener(achievements);
        achievements5 = root.findViewById(R.id.achievements5);
        achievements5.setOnClickListener(achievements);

        loading = root.findViewById(R.id.loading);

        //проверяем, есть ли никнейм (иначе показывается заглушка)
        if (!noNickname) {
            setHasOptionsMenu(true); //добавляем меню в Action Bar
            showFriends(); //загружаем список друзей
            loading.setVisibility(View.VISIBLE); //анимация загрузки
        }

        newRequest = root.findViewById(R.id.newRequest);
        newRequest.setOnClickListener(openNewRequests);

        //проверяем, есть ли новые заявки в друзья
        friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        friendsDatabase.child("incoming").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null)
                            newRequest.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        errorMessage.setText("Неизвестная ошибка");
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        onStart();
        //проверяем, появился ли никнейм (если пользователь заходил в настройки)
        if (!nicknamePref.getString("nickname", "0").equals("0")) {
            errorMessage.setVisibility(View.GONE);
            noNickname = false;
            setHasOptionsMenu(true);
        }

        //проверяем, остались ли заявки (если нет -- прячем кнопку)
        friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0"));
        friendsDatabase.child("incoming").addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null)
                            newRequest.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        errorMessage.setText("Неизвестная ошибка");
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                });

        //прячем старые лэйауты (чтобы обновить список друзей, если он обновился)
        layout1.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);
        layout3.setVisibility(View.GONE);
        layout4.setVisibility(View.GONE);
        layout5.setVisibility(View.GONE);

        //показываем друзей
        showFriends();
    }

    //создаём меню в action bar
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.friends_menu, menu);
    }

    //объявляем кнопку поиска
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //проверяем макс. кол-во друзей (5)
        if (layout5.getVisibility() == View.VISIBLE) {
            Snackbar.make(root, "Достигнуто максимальное количество друзей", Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.nav_view)
                    .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                    .setTextColor(getResources().getColor(R.color.colorContrast))
                    .show();
            return super.onOptionsItemSelected(item);
        }
        //передаём список друзей в активити поиска, и запускаем поиск
        if (item.getItemId() == R.id.search) {
            friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("friends");
            friendsDatabase.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Intent intent = new Intent(getActivity(), FriendsSearch.class);
                            Bundle bundleFriends = new Bundle();
                            if (dataSnapshot.getValue() != null) {
                                allFriends = dataSnapshot.getValue(String.class);
                                allFriendsList = new ArrayList<>();
                                allFriendsList.clear();
                                while (allFriends.contains(",")) {
                                    allFriendsList.add(allFriends.substring(0, allFriends.indexOf(",")));
                                    allFriends = allFriends.substring(allFriends.indexOf(",") + 1);
                                }
                                allFriendsList.add(allFriends);
                                bundleFriends.putStringArrayList("friendsList", (ArrayList<String>) allFriendsList);
                            } else
                                bundleFriends.putStringArrayList("friendsList", new ArrayList<String>());

                            intent.putExtras(bundleFriends);
                            startActivity(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //контестное меню для удаления друга
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
            case R.id.friend1:
                menu.add(0, 1, 0, "Удалить");
                break;
            case R.id.friend2:
                menu.add(0, 2, 0, "Удалить");
                break;
            case R.id.friend3:
                menu.add(0, 3, 0, "Удалить");
                break;
            case R.id.friend4:
                menu.add(0, 4, 0, "Удалить");
                break;
            case R.id.friend5:
                menu.add(0, 5, 0, "Удалить");
                break;
        }
    }

    //удаление друга
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String deleteNickname = "";
        switch (item.getItemId()) {
            case 1:
                layout1.setVisibility(View.GONE);
                deleteNickname = friend1.getText().toString();
                break;
            case 2:
                layout2.setVisibility(View.GONE);
                deleteNickname = friend2.getText().toString();
                break;
            case 3:
                layout3.setVisibility(View.GONE);
                deleteNickname = friend3.getText().toString();
                break;
            case 4:
                layout4.setVisibility(View.GONE);
                deleteNickname = friend4.getText().toString();
                break;
            case 5:
                layout5.setVisibility(View.GONE);
                deleteNickname = friend5.getText().toString();
                break;
        }
        getIdByNickname(deleteNickname, new Callback() {
            @Override
            public void onCallback(final String id) {
                final DatabaseReference friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("friends");
                friendsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                        String newString;
                        if (Objects.requireNonNull(dataSnapshot2.getValue()).toString().contains("," + id))
                            newString = dataSnapshot2.getValue().toString().replace("," + id, "");
                        else if (Objects.requireNonNull(dataSnapshot2.getValue()).toString().contains(id + ","))
                            newString = dataSnapshot2.getValue().toString().replace(id + ",", "");
                        else {
                            friendsDatabase.removeValue();
                            return;
                        }
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
                        if (Objects.requireNonNull(dataSnapshot2.getValue()).toString().contains("," + nicknamePref.getString("id", "0")))
                            newString = dataSnapshot2.getValue().toString().replace("," + nicknamePref.getString("id", "0"), "");
                        else if (Objects.requireNonNull(dataSnapshot2.getValue()).toString().contains(nicknamePref.getString("id", "0") + ","))
                            newString = dataSnapshot2.getValue().toString().replace(nicknamePref.getString("id", "0") + ",", "");
                        else {
                            secondFriendsDatabase.removeValue();
                            return;
                        }
                        secondFriendsDatabase.setValue(newString);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        //показываем снекбар об удалении друга
        Snackbar.make(root, "Друг удалён", Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.nav_view)
                .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
                .setTextColor(getResources().getColor(R.color.colorContrast))
                .show();

        return super.onContextItemSelected(item);
    }

    //открываем новые заявки в друзья
    View.OnClickListener openNewRequests = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), FriendsIncomingRequests.class);
            startActivity(intent);
        }
    };

    //показываем друзей
    private void showFriends() {
        allFriendsList = new ArrayList<>();
        final DatabaseReference temp = friendsDatabase;
        friendsDatabase = FirebaseDatabase.getInstance().getReference(nicknamePref.getString("id", "0")).child("friends");
        friendsDatabase.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (temp == friendsDatabase || layout1.getVisibility() == View.VISIBLE)
                            return;
                        errorMessage.setVisibility(View.GONE);
                        allFriends = dataSnapshot.getValue(String.class);
                        if (allFriends == null) {
                            if (!noNickname)
                                errorMessage.setText("У тебя пока что нет друзей");
                            errorMessage.setVisibility(View.VISIBLE);
                        }
                        else {
                            allFriendsList.clear();
                            allFriendsList.add("");
                            while (allFriends.contains(",")) {
                                allFriendsList.add(allFriends.substring(0, allFriends.indexOf(",")));
                                allFriends = allFriends.substring(allFriends.indexOf(",") + 1);
                            }
                            allFriendsList.add(allFriends);
                            for (int i = 1; i < allFriendsList.size(); i++) {
                                switch (i) {
                                    case 1:
                                        getNicknameById(allFriendsList.get(1), new Callback() {
                                            @Override
                                            public void onCallback(String value) {
                                                layout1.setVisibility(View.VISIBLE);
                                                friend1.setText(value);
                                                habits1.setTag(allFriendsList.get(1));
                                                achievements1.setTag(allFriendsList.get(1));

                                            }
                                        });
                                        break;
                                    case 2:
                                        getNicknameById(allFriendsList.get(2), new Callback() {
                                            @Override
                                            public void onCallback(String value) {
                                                layout2.setVisibility(View.VISIBLE);
                                                friend2.setText(value);
                                                habits2.setTag(allFriendsList.get(2));
                                                achievements2.setTag(allFriendsList.get(2));
                                            }
                                        });
                                        break;
                                    case 3:
                                        getNicknameById(allFriendsList.get(3), new Callback() {
                                            @Override
                                            public void onCallback(String value) {
                                                layout3.setVisibility(View.VISIBLE);
                                                friend3.setText(value);
                                                habits3.setTag(allFriendsList.get(3));
                                                achievements3.setTag(allFriendsList.get(3));
                                            }
                                        });
                                        break;
                                    case 4:
                                        getNicknameById(allFriendsList.get(4), new Callback() {
                                            @Override
                                            public void onCallback(String value) {
                                                layout4.setVisibility(View.VISIBLE);
                                                friend4.setText(value);
                                                habits4.setTag(allFriendsList.get(4));
                                                achievements4.setTag(allFriendsList.get(4));
                                            }
                                        });
                                        break;
                                    case 5:
                                        getNicknameById(allFriendsList.get(5), new Callback() {
                                            @Override
                                            public void onCallback(String value) {
                                                layout5.setVisibility(View.VISIBLE);
                                                friend5.setText(value);
                                                habits5.setTag(allFriendsList.get(5));
                                                achievements5.setTag(allFriendsList.get(5));
                                            }
                                        });
                                        break;
                                }
                            }
                        }
                        //останавливаем анимацию
                        if (loading.getVisibility() == View.VISIBLE) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (getContext() == null)
                                        return;
                                    Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.vanish);
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
                //останавливаем анимацию (если больше 5 сек. не загрузилось) + показываем ошибку)
                if (loading.getVisibility() == View.VISIBLE && getContext() != null) {
                    Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.vanish);
                    loading.startAnimation(anim);
                    errorMessage.setText("Нет подключения к интернету");
                    errorMessage.setVisibility(View.VISIBLE);
                    Handler handler1 = new Handler();
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loading.setVisibility(View.GONE);
                        }
                    }, 500);
                }
            }
        }, 5000);
    }

    //метод, который возвращает никнейм по айди пользователя
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
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        errorMessage.setText("Неизвестная ошибка");
                        errorMessage.setVisibility(View.VISIBLE);
                    }
                });
    }

    //метод, который возвращает айди по никнейму пользователя
    private void getIdByNickname(String nickname, final Callback callback) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
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

    //открываем привычки друга
    View.OnClickListener habits = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            getNicknameById(v.getTag().toString(), new Callback() {
                @Override
                public void onCallback(String value) {
                    Bundle bundleId = new Bundle();
                    bundleId.putString("id", v.getTag().toString());
                    Bundle bundleNickname = new Bundle();
                    bundleNickname.putString("nickname", value);
                    Intent intent = new Intent(getActivity(), FriendsHabits.class);
                    intent.putExtras(bundleId);
                    intent.putExtras(bundleNickname);
                    startActivity(intent);
                }
            });
        }
    };

    //открываем достижения друга
    View.OnClickListener achievements = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            getNicknameById(v.getTag().toString(), new Callback() {
                @Override
                public void onCallback(String value) {
                    Bundle bundleId = new Bundle();
                    bundleId.putString("id", v.getTag().toString());
                    Bundle bundleNickname = new Bundle();
                    bundleNickname.putString("nickname", value);
                    Intent intent = new Intent(getActivity(), FriendsAchievements.class);
                    intent.putExtras(bundleId);
                    intent.putExtras(bundleNickname);
                    startActivity(intent);
                }
            });
        }
    };
}