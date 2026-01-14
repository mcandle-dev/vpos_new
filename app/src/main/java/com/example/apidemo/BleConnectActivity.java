package com.example.apidemo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apidemo.ble.BleConnection;
import com.example.apidemo.model.Member;
import com.mcandle.vpos.R;

/**
 * BLE Connection Activity - Full screen activity for BLE device connection
 * Displays customer benefits and payment options
 */
public class BleConnectActivity extends AppCompatActivity {

    private static final String TAG = "BleConnectActivity";

    // Intent extras
    public static final String EXTRA_DEVICE_MAC = "EXTRA_DEVICE_MAC";
    public static final String EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME";
    public static final String EXTRA_SERVICE_UUID = "EXTRA_SERVICE_UUID";

    // Views
    private TextView tvCustomerNameLabel;
    private TextView tvDeviceMac;
    private TextView tvCustomerPointsValue;
    private View viewStatusIndicator;
    private TextView tvBottomStatus;
    private Button btnCardPayment;
    private Button btnAppPayment;
    private Button btnBack;

    // Data
    private String deviceMac;
    private String deviceName;
    private String serviceUuid;
    private Member member;
    private BleConnection bleConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_connect);

        // Hide ActionBar - using custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Get device info from intent
        deviceMac = getIntent().getStringExtra(EXTRA_DEVICE_MAC);
        deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        serviceUuid = getIntent().getStringExtra(EXTRA_SERVICE_UUID);

        // Initialize member with hardcoded data (to be fetched from server in future)
        member = new Member();
        if (serviceUuid != null && !serviceUuid.isEmpty()) {
            member.setServiceUuid(serviceUuid);
        }

        // Initialize BLE connection
        bleConnection = new BleConnection();

        // Initialize views
        initViews();

        // Setup click listeners
        setupClickListeners();

        // Display member info
        displayMemberInfo();

        // Auto-connect to BLE device
        autoConnectBle();
    }

    private void initViews() {
        tvCustomerNameLabel = findViewById(R.id.tvCustomerNameLabel);
        tvDeviceMac = findViewById(R.id.tvDeviceMac);
        tvCustomerPointsValue = findViewById(R.id.tvCustomerPointsValue);
        viewStatusIndicator = findViewById(R.id.viewStatusIndicator);
        tvBottomStatus = findViewById(R.id.tvBottomStatus);
        btnCardPayment = findViewById(R.id.btnCardPayment);
        btnAppPayment = findViewById(R.id.btnAppPayment);
        btnBack = findViewById(R.id.btnBack);

        // Initially disable app payment until connected
        btnAppPayment.setEnabled(false);
    }

    private void displayMemberInfo() {
        // Display customer name
        tvCustomerNameLabel.setText(member.getDisplayName());

        // Display grade and member ID (or serviceUUID if available)
        if (member.getServiceUuid() != null && !member.getServiceUuid().isEmpty()) {
            tvDeviceMac.setText(member.getGrade() + " ⭐ | " + member.getServiceUuid());
        } else {
            tvDeviceMac.setText(member.getDisplayGradeInfo());
        }

        // Display points
        tvCustomerPointsValue.setText(member.getDisplayPoints());
    }

    private void setupClickListeners() {
        // Back button - return to BeaconActivity
        btnBack.setOnClickListener(v -> {
            disconnectAndFinish();
        });

        // Card Payment button - Navigate to PaymentActivity
        btnCardPayment.setOnClickListener(v -> {
            Intent intent = new Intent(BleConnectActivity.this, PaymentActivity.class);
            intent.putExtra("EXTRA_MODE", "OFFLINE");
            intent.putExtra("EXTRA_AMOUNT", 314100); // Final price with discount
            intent.putExtra("EXTRA_PRODUCT_NAME", "스포츠 상품");
            startActivity(intent);
        });

        // App Payment button - Send BLE data then navigate to PaymentActivity
        btnAppPayment.setOnClickListener(v -> {
            if (!bleConnection.isConnected()) {
                tvBottomStatus.setText("BLE 연결 필요");
                tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
                return;
            }

            btnAppPayment.setEnabled(false);
            tvBottomStatus.setText("앱 결제 요청 중...");
            tvBottomStatus.setTextColor(Color.parseColor("#FF9800"));

            new Thread(() -> {
                // Send order_id=1234 via BLE
                String sendData = "order_id=1234";
                Log.d(TAG, "Sending BLE data: " + sendData);

                BleConnection.SendResult result = bleConnection.sendDataCompleteByMservice(sendData, 4000);

                runOnUiThread(() -> {
                    btnAppPayment.setEnabled(true);

                    if (result.isSuccess()) {
                        Log.d(TAG, "BLE send success");
                        tvBottomStatus.setText("앱 결제 요청 전송됨");
                        tvBottomStatus.setTextColor(Color.parseColor("#4CAF50"));

                        // Navigate to PaymentActivity
                        Intent intent = new Intent(BleConnectActivity.this, PaymentActivity.class);
                        intent.putExtra("EXTRA_MODE", "APP");
                        intent.putExtra("EXTRA_AMOUNT", 349000); // Original price
                        intent.putExtra("EXTRA_PRODUCT_NAME", "스포츠 상품");
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "BLE send failed: " + result.getError());
                        tvBottomStatus.setText("전송 실패: " + result.getError());
                        tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
                    }
                });
            }).start();
        });
    }

    private void autoConnectBle() {
        if (deviceMac == null || deviceMac.isEmpty()) {
            Log.e(TAG, "No device MAC address provided");
            tvBottomStatus.setText("디바이스 정보 없음");
            tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
            return;
        }

        // Show connecting status
        viewStatusIndicator.setBackgroundResource(R.drawable.circle_red);
        tvBottomStatus.setText("연결 중...");
        tvBottomStatus.setTextColor(Color.parseColor("#FF9800"));

        new Thread(() -> {
            Log.d(TAG, "Auto-connecting to device: " + deviceMac);

            // Execute connection
            BleConnection.ConnectionResult result = bleConnection.connectToDeviceByMservice(deviceMac);

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Log.d(TAG, "BLE connection successful");
                    viewStatusIndicator.setBackgroundResource(R.drawable.circle_green);
                    tvBottomStatus.setText("연결됨");
                    tvBottomStatus.setTextColor(Color.parseColor("#4CAF50"));
                    btnAppPayment.setEnabled(true);
                } else {
                    Log.e(TAG, "BLE connection failed: " + result.getError());
                    viewStatusIndicator.setBackgroundResource(R.drawable.circle_red);
                    tvBottomStatus.setText("연결 실패");
                    tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
                    btnAppPayment.setEnabled(false);
                }
            });
        }).start();
    }

    private void disconnectAndFinish() {
        if (bleConnection != null && bleConnection.isConnected()) {
            new Thread(() -> {
                Log.d(TAG, "Disconnecting BLE...");
                bleConnection.disconnect();
                runOnUiThread(this::finish);
            }).start();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        disconnectAndFinish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure disconnection when activity is destroyed
        if (bleConnection != null && bleConnection.isConnected()) {
            new Thread(() -> bleConnection.disconnect()).start();
        }
    }
}
