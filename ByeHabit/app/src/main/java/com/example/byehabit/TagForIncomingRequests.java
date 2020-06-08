package com.example.byehabit;

import android.widget.LinearLayout;

public class TagForIncomingRequests {
    private LinearLayout layoutOfRequest;
    private String idOfRequest;

    //этот класс используется для хранения в кнопках информации о заявке в друзья

    public TagForIncomingRequests (String idOfRequest, LinearLayout layoutOfRequest) {
        this.layoutOfRequest = layoutOfRequest;
        this.idOfRequest = idOfRequest;
    }

    public LinearLayout getLayoutOfRequest() {
        return layoutOfRequest;
    }

    public String getIdOfRequest() {
        return idOfRequest;
    }
}
