package com.example.offlinechat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiDirectReceiver receiver;

    private Button btnDiscover;
    private TextView tvStatus;
    private ListView deviceListView;

    private ArrayAdapter<String> deviceAdapter;
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private final IntentFilter intentFilter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDiscover = findViewById(R.id.btnDiscover);
        tvStatus = findViewById(R.id.tvStatus);
        deviceListView = findViewById(R.id.deviceListView);

        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(this, getMainLooper(), null);
        }

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        deviceListView.setAdapter(deviceAdapter);

        receiver = new WifiDirectReceiver(manager, channel, this);

        checkPermissions();

        btnDiscover.setOnClickListener(v -> {
            if (!hasPermission()) {
                checkPermissions();
                return;
            }
            if (!isWifiEnabled()) {
                Toast.makeText(this, "Please turn ON Wi-Fi", Toast.LENGTH_LONG).show();
                return;
            }
            if (!isLocationEnabled()) {
                Toast.makeText(this, "Please turn ON Location/GPS", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } catch (Exception ignored) {}
                return;
            }
            resetAndDiscover();
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < peers.size()) {
                WifiP2pDevice device = peers.get(position);
                connectToDevice(device);
            }
        });
    }

    // =====================================================
    // RESET P2P SYSTEM & DISCOVER (Fixes BUSY Error Code 2)
    // =====================================================
    private void resetAndDiscover() {
        if (manager == null || channel == null) return;

        updateStatus("Resetting Wi-Fi Direct connection...");

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                startDiscoveryProcess();
            }

            @Override
            public void onFailure(int reason) {
                startDiscoveryProcess();
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void startDiscoveryProcess() {
        deviceAdapter.clear();
        peers.clear();
        deviceAdapter.notifyDataSetChanged();

        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                initiateDiscovery();
            }

            @Override
            public void onFailure(int reason) {
                initiateDiscovery();
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void initiateDiscovery() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                updateStatus("Searching for nearby devices...");
            }

            @Override
            public void onFailure(int reason) {
                updateStatus("Discovery failed: " + reason);
            }
        });
    }

    // =====================================================
    // CONNECT TO DEVICE
    // =====================================================
    @SuppressWarnings("MissingPermission")
    private void connectToDevice(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        updateStatus("Connecting to " + device.deviceName + "...");

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Connection request sent to " + device.deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                updateStatus("Connection failed: " + reason);
            }
        });
    }

    // =====================================================
    // CONNECTION SUCCESS -> GO TO CHAT SCREEN
    // =====================================================
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (info.groupFormed) {
            updateStatus("Connected! Opening Chat...");

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("isHost", info.isGroupOwner);
            if (info.groupOwnerAddress != null) {
                intent.putExtra("hostAddress", info.groupOwnerAddress.getHostAddress());
            }
            startActivity(intent);
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        List<WifiP2pDevice> newPeers = new ArrayList<>(peerList.getDeviceList());
        peers.clear();
        peers.addAll(newPeers);
        deviceAdapter.clear();

        if (peers.isEmpty()) {
            deviceAdapter.add("No nearby devices found");
            updateStatus("No devices found");
        } else {
            for (WifiP2pDevice device : peers) {
                deviceAdapter.add(device.deviceName + "\n" + getDeviceStatus(device));
            }
            updateStatus(peers.size() + " device(s) found");
        }
        deviceAdapter.notifyDataSetChanged();
    }

    private String getDeviceStatus(WifiP2pDevice device) {
        switch (device.status) {
            case WifiP2pDevice.AVAILABLE: return "Available";
            case WifiP2pDevice.INVITED: return "Invited";
            case WifiP2pDevice.CONNECTED: return "Connected";
            case WifiP2pDevice.FAILED: return "Failed";
            case WifiP2pDevice.UNAVAILABLE: return "Unavailable";
            default: return "Unknown";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, receiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void checkPermissions() {
        if (!hasPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.NEARBY_WIFI_DEVICES,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_MEDIA_IMAGES
                }, PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean isWifiEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    public void updateStatus(String message) {
        runOnUiThread(() -> tvStatus.setText("Status: " + message));
    }
}