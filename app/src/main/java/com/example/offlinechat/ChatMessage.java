package com.example.offlinechat;

public class ChatMessage {
    private String message;
    private String imageUri; // Bitmap-এর পরিবর্তে String Uri
    private boolean isSentByMe;

    // Text Message Constructor
    public ChatMessage(String message, boolean isSentByMe) {
        this.message = message;
        this.isSentByMe = isSentByMe;
        this.imageUri = null;
    }

    // Image Message Constructor
    public ChatMessage(String imageUri, boolean isSentByMe, boolean isImage) {
        this.imageUri = imageUri;
        this.isSentByMe = isSentByMe;
        this.message = null;
    }

    public String getMessage() { return message; }
    public String getImageUri() { return imageUri; }
    public boolean isSentByMe() { return isSentByMe; }
}