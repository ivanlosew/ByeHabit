package com.example.byehabit;

public interface Callback {
    void onCallback(String value);
    //этот интерфейс нужен для решения проблемы асинхронности онлайн базы данных
}