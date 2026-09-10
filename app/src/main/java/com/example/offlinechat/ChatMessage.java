package com.example.offlinechat;

public class ChatMessage {
    private String message;
    private boolean isSent;
    private boolean isImage;

    // টেক্সট মেসেজের জন্য কনস্ট্রাক্টর
    public ChatMessage(String message, boolean isSent) {
        this.message = message;
        this.isSent = isSent;
        this.isImage = false;
    }

    // ইমেজ মেসেজের জন্য কনস্ট্রাক্টর
    public ChatMessage(String message, boolean isSent, boolean isImage) {
        this.message = message;
        this.isSent = isSent;
        this.isImage = isImage;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSent() {
        return isSent;
    }

    public boolean isSentByMe() {
        return isSent;
    }

    public boolean isImage() {
        return isImage;
    }

    // ChatAdapter-এর getImageUri() এরর ঠিক করার জন্য হেল্পার মেথড
    public String getImageUri() {
        if (isImage) {
            return message;
        }
        return null;
    }
}