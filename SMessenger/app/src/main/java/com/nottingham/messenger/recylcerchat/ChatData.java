package com.nottingham.messenger.recylcerchat;

import com.nottingham.messenger.activities.ConversationActivity;

import java.util.Date;

public class ChatData {

    private String type;
    private String text;
    private String time;

    public ChatData(String type, String text) {
        this.type = type;
        this.text = text;
        this.time = ConversationActivity.HOURS_AND_MINUTES_DATE_FORMAT.format(new Date());
    }

    public ChatData() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
