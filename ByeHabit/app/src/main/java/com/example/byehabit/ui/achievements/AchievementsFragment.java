package com.example.byehabit.ui.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.byehabit.R;

public class AchievementsFragment extends Fragment {

    private SharedPreferences achPref; //SP ачивок
    private ImageView achievementImage1, achievementImage2, achievementImage3;
    private TextView achievementTitle1, achievementTitle2, achievementTitle3;

    private static final int NOVICHOK = 1, BYVALYI = 2, DESYATKA = 3;
    private boolean[] isSet = new boolean[4];

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_achievements, container, false);
        achPref = requireContext().getSharedPreferences("achPref", Context.MODE_PRIVATE);

        achievementImage1 = root.findViewById(R.id.achievementImage1);
        achievementImage2 = root.findViewById(R.id.achievementImage2);
        achievementImage3 = root.findViewById(R.id.achievementImage3);
        achievementTitle1 = root.findViewById(R.id.achievementTitle1);
        achievementTitle2 = root.findViewById(R.id.achievementTitle2);
        achievementTitle3 = root.findViewById(R.id.achievementTitle3);

        isSet[1] = false; isSet[2] = false; isSet[3] = false;

        //ставим достижения, полученные пользователем
        for (int i = 1; i <= 3; i++) {
            if (achPref.getBoolean("Новичок", false) && !isSet[NOVICHOK]) {
                setAchievement("Новичок", R.drawable.ach_novichok, i);
                isSet[NOVICHOK] = true;
            }
            else if (achPref.getBoolean("Бывалый", false) && !isSet[BYVALYI]) {
                setAchievement("Бывалый", R.drawable.ach_byvaliy, i);
                isSet[BYVALYI] = true;
            }
            else if (achPref.getBoolean("Десятка", false) && !isSet[DESYATKA]) {
                setAchievement("Десятка", R.drawable.ach_desyatka, i);
                isSet[DESYATKA] = true;
            }
        }

        return root;
    }

    //вспомогательный метод, чтобы поставить конкретную привычку
    public void setAchievement (String achievement, int drawable, int position) {
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

/*
Новичок -- добавить первую привычку
Бывалый -- добавить три привычки
Десятка -- десять дней как бросил привычку
 */
