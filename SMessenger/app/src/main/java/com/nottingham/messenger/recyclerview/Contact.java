package com.nottingham.messenger.recyclerview;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class Contact {
    private int id;
    private String name;
    private List<String> phoneNumbers = new ArrayList<>();
    private Bitmap image = null;
    private Uri imageURI = null;

    public void setImageURI(Uri imageURI) {
        this.imageURI = imageURI;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addPhone(String phone) {
        phoneNumbers.add(phone);
    }

    public boolean hasImage() {
        return image != null || imageURI != null;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap img) {
        image = img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getImageURI() {
        return imageURI;
    }
}
