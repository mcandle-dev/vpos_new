package com.example.apidemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apidemo.ble.BleConnection;

import java.text.NumberFormat;
import java.util.Locale;

import com.mcandle.vpos.R;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    private TextView tvPaymentTitle;
    private TextView tvPaymentAmount;
    private TextView tvPaymentStatusMsg;
    private TextView tvPaymentDesc;
    private ImageView ivPaymentIcon;
    private Button btnBack;
    private Button btnCompletePayment;
    private ProgressBar pbWaiting;

    private String mode;
    private int amount;
    private String productName;
    private BleConnection bleConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Get intent extras
        mode = getIntent().getStringExtra("EXTRA_MODE");
        if (mode == null) mode = "OFFLINE";

        amount = getIntent().getIntExtra("EXTRA_AMOUNT", 0);
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        if (productName == null) productName = "상품";

        // Get shared BLE connection
        bleConnection = BleConnectActivity.getSharedBleConnection();

        // Initialize views
        tvPaymentTitle = findViewById(R.id.tvPaymentTitle);
        tvPaymentAmount = findViewById(R.id.tvPaymentAmount);
        tvPaymentStatusMsg = findViewById(R.id.tvPaymentStatusMsg);
        tvPaymentDesc = findViewById(R.id.tvPaymentDesc);
        ivPaymentIcon = findViewById(R.id.ivPaymentIcon);
        btnBack = findViewById(R.id.btnBack);
        btnCompletePayment = findViewById(R.id.btnCompletePayment);
        pbWaiting = findViewById(R.id.pbWaiting);

        setupUI();

        btnBack.setOnClickListener(v -> finish());

        btnCompletePayment.setOnClickListener(v -> {
            sendFinishNotification();
        });

        // Auto complete for APP mode after 2 seconds
        if ("APP".equals(mode)) {
            new android.os.Handler().postDelayed(this::navigateToSuccess, 2000);
        }
    }

    private void setupUI() {
        tvPaymentAmount.setText(formatNumber(amount) + "원");

        if ("APP".equals(mode)) {
            tvPaymentTitle.setText("앱 결제 대기");
            tvPaymentStatusMsg.setText("고객 앱으로 주문 정보가\n전송되었습니다");
            tvPaymentDesc.setText("고객이 앱에서 결제 중입니다...");
            btnCompletePayment.setVisibility(View.GONE);
            pbWaiting.setVisibility(View.VISIBLE);
        } else {
            tvPaymentTitle.setText("카드 결제");
            tvPaymentStatusMsg.setText("카드를 단말기에 넣어주세요");
            tvPaymentDesc.setText("현대백화점 카드 전용 혜택 적용됨");
            btnCompletePayment.setVisibility(View.VISIBLE);
            pbWaiting.setVisibility(View.GONE);
        }
    }

    /**
     * Send finish notification via BLE and navigate to success screen
     */
    private void sendFinishNotification() {
        // Check if BLE is connected
        if (bleConnection != null && bleConnection.isConnected()) {
            // Disable button to prevent multiple clicks
            btnCompletePayment.setEnabled(false);
            btnCompletePayment.setText("전송 중...");

            new Thread(() -> {
                // Send order_id=finish via BLE
                String sendData = "order_id=finish";
                Log.d(TAG, "Sending BLE data: " + sendData);

                BleConnection.SendResult result = bleConnection.sendDataCompleteByMservice(sendData, 4000);

                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        Log.d(TAG, "BLE send success");
                        navigateToSuccess();
                    } else {
                        Log.e(TAG, "BLE send failed: " + result.getError());
                        // Still navigate to success even if BLE send fails
                        navigateToSuccess();
                    }
                });
            }).start();
        } else {
            // No BLE connection, just navigate to success
            Log.d(TAG, "No BLE connection, navigating to success");
            navigateToSuccess();
        }
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(this, SuccessActivity.class);
        intent.putExtra("EXTRA_PRODUCT_NAME", productName);
        intent.putExtra("EXTRA_AMOUNT", amount);
        startActivity(intent);
        finish();
    }

    private String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number);
    }
}
