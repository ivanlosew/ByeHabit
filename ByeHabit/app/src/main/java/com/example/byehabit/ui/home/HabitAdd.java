package com.example.byehabit.ui.home;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.byehabit.R;

import java.util.Objects;

public class HabitAdd extends AppCompatActivity {

    private ImageButton done;
    private EditText newHabit;
    private DBHelper dbHelper;
    private SharedPreferences sPref;
    private TextView titleAdding;
    private static final String APP_PREFERENCES_GENDER = "GENDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adding_habit);

        newHabit = findViewById(R.id.newHabit);
        done = findViewById(R.id.done);
        done.setOnClickListener(AddingNewHabit);
        dbHelper = new DBHelper(this);

        //меняем заголовок если пол Ж
        sPref = Objects.requireNonNull(getApplicationContext()).getSharedPreferences("sPref", Context.MODE_PRIVATE);
        if (sPref.getString(APP_PREFERENCES_GENDER, "").equals("FEMALE")) {
            titleAdding = findViewById(R.id.titleAdding);
            titleAdding.setText(R.string.title_habits_w);
        }
    }

    //добавляем привычку
    View.OnClickListener AddingNewHabit = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();

            String label = newHabit.getText().toString();
            label = label.toLowerCase();
            label = label.trim().replaceAll(" +", " ");
            if (label.equals("")) {
                setResult(2);
                finish();
                return;
            }
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.query("habits", null, null, null, null, null, null);
            if (c.getCount() == 3) {
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put("label", label);
            cv.put("created", System.currentTimeMillis() / 1000);
            cv.put("fails", 0);
            cv.put("lastFail", "-");
            cv.put("lastSuc", System.currentTimeMillis() - 86400000);
            db.insert("habits", null, cv);
            db.close();

            //передаём фрагменту, что привычка добавлена
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    //БАЗА ДАННЫХ (оффлайн)
    static public class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table habits ("
                    + "id integer primary key autoincrement,"
                    + "label text,"
                    + "created long,"
                    + "fails integer,"
                    + "lastFail text,"
                    + "lastSuc long"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
