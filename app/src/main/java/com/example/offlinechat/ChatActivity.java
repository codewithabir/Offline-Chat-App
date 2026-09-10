package com.example.offlinechat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int PORT = 8888;

    private TextView tvChatStatus;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnAttach;
    private RecyclerView rvMessages;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvChatStatus = findViewById(R.id.tvChatStatus);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);
        rvMessages = findViewById(R.id.rvMessages);

        chatAdapter = new ChatAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(chatAdapter);

        boolean isHost = getIntent().getBooleanExtra("isHost", false);
        String hostAddress = getIntent().getStringExtra("hostAddress");

        if (isHost) {
            tvChatStatus.setText("Status: Server Ready (Waiting...)");
            startServer();
        } else {
            tvChatStatus.setText("Status: Connecting to Server...");
            connectToServer(hostAddress);
        }

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                if (isConnected && dataOutputStream != null) {
                    sendMessage(msg);
                    etMessage.setText("");
                } else {
                    Toast.makeText(this, "Connection not ready yet!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAttach.setOnClickListener(v -> openGallery());
    }

    // =====================================================
    // SERVER SOCKET (HOST)
    // =====================================================
    private void startServer() {
        executorService.execute(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                serverSocket = new ServerSocket(PORT);
                socket = serverSocket.accept();
                setupStreams();
            } catch (Exception e) {
                Log.e(TAG, "Server Error", e);
                updateStatusOnUI("Connection failed: Server error");
            }
        });
    }

    // =====================================================
    // CLIENT SOCKET (CLIENT)
    // =====================================================
    private void connectToServer(String hostAddress) {
        executorService.execute(() -> {
            int attempts = 0;
            while (attempts < 5 && !isConnected) {
                try {
                    attempts++;
                    Thread.sleep(1000);
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(hostAddress, PORT), 5000);
                    setupStreams();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Client Connection Attempt " + attempts + " failed");
                }
            }
            if (!isConnected) {
                updateStatusOnUI("Connection failed: Server unreachable");
            }
        });
    }

    private void setupStreams() {
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            isConnected = true;

            updateStatusOnUI("Connected & Ready to Chat!");
            listenForIncomingData();
        } catch (Exception e) {
            Log.e(TAG, "Stream Setup Error", e);
            updateStatusOnUI("Failed to setup communication streams");
        }
    }

    // =====================================================
    // SAFE SEND MESSAGE (NO CRASH)
    // =====================================================
    private void sendMessage(String msg) {
        executorService.execute(() -> {
            try {
                if (dataOutputStream != null && isConnected) {
                    dataOutputStream.writeUTF("TEXT");
                    dataOutputStream.writeUTF(msg);
                    dataOutputStream.flush();

                    addMessageToUI(new ChatMessage(msg, true));
                }
            } catch (Exception e) {
                Log.e(TAG, "Send Error", e);
                showToastOnUI("Failed to send message. Connection lost.");
            }
        });
    }

    // =====================================================
    // SAFE SEND IMAGE
    // =====================================================
    private void sendImage(Uri imageUri, Bitmap bitmap) {
        if (!isConnected || dataOutputStream == null) return;

        executorService.execute(() -> {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();

                dataOutputStream.writeUTF("IMAGE");
                dataOutputStream.writeInt(bytes.length);
                dataOutputStream.write(bytes);
                dataOutputStream.flush();

                // Bitmap-এর পরিবর্তে String Uri পাঠানো হচ্ছে
                addMessageToUI(new ChatMessage(imageUri.toString(), true, true));
            } catch (Exception e) {
                Log.e(TAG, "Image Send Error", e);
                showToastOnUI("Failed to send image.");
            }
        });
    }

    // =====================================================
    // SAFE INCOMING DATA LISTEN
    // =====================================================
    private void listenForIncomingData() {
        executorService.execute(() -> {
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    String type = dataInputStream.readUTF();

                    if ("TEXT".equals(type)) {
                        String msg = dataInputStream.readUTF();
                        addMessageToUI(new ChatMessage(msg, false));

                    } else if ("IMAGE".equals(type)) {
                        int length = dataInputStream.readInt();
                        byte[] bytes = new byte[length];
                        dataInputStream.readFully(bytes);

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            // প্রাপ্ত Bitmap-কে লোকাল ক্যাশে সেভ করে তার URI নেওয়া হচ্ছে
                            String savedImagePath = saveBitmapToCache(bitmap);
                            if (savedImagePath != null) {
                                addMessageToUI(new ChatMessage(savedImagePath, false, true));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Listen Error", e);
                    isConnected = false;
                    updateStatusOnUI("Disconnected");
                    break;
                }
            }
        });
    }

    // Bitmap-কে Temp ফাইল হিসেবে সেভ করার হেল্পার মেথড
    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "received_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image", e);
            return null;
        }
    }

    // =====================================================
    // SAFE UI UPDATES (Crash-Proof)
    // =====================================================
    private void addMessageToUI(ChatMessage message) {
        mainHandler.post(() -> {
            try {
                messageList.add(message);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvMessages.scrollToPosition(messageList.size() - 1);
            } catch (Exception e) {
                Log.e(TAG, "UI Update Error", e);
            }
        });
    }

    private void updateStatusOnUI(String statusText) {
        mainHandler.post(() -> {
            if (tvChatStatus != null) {
                tvChatStatus.setText("Status: " + statusText);
            }
        });
    }

    private void showToastOnUI(String message) {
        mainHandler.post(() -> Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void openGallery() {
        // Android 13+ (API level 33+) এর জন্য READ_MEDIA_IMAGES চেক
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 102);
                return;
            }
        } else {
            // Android 12 ও তার নিচের জন্য READ_EXTERNAL_STORAGE চেক
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
                return;
            }
        }

        // পারমিশন দেওয়া থাকলে সরাসরি গ্যালারি খুলবে
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                sendImage(imageUri, bitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
        try {
            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            executorService.shutdown();
        } catch (IOException ignored) {}
    }
}