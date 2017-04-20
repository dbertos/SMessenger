package com.nottingham.messenger.recyclerview;

public class Chat {

    private String displayName = null;
    private String address;
    private String lastChat;
    private String time;
    private int image;
    private boolean online;

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLastChat() {
        return lastChat;
    }

    public void setLastChat(String lastChat) {
        this.lastChat = lastChat;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public boolean getOnline() {
        return online;
    }

    public void setOnline(boolean on) {
        online = on;
    }
}