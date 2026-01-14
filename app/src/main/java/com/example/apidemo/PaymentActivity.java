package com.example.apidemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

import com.mcandle.vpos.R;

public class PaymentActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Get intent extras
        mode = getIntent().getStringExtra("EXTRA_MODE");
        if (mode == null) mode = "OFFLINE";

        amount = getIntent().getIntExtra("EXTRA_AMOUNT", 0);
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        if (productName == null) productName = "상품";

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
            navigateToSuccess();
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
