package com.example.apidemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apidemo.model.Order;

import java.text.NumberFormat;
import java.util.Locale;

import com.mcandle.vpos.R;

public class  SuccessActivity extends AppCompatActivity {

    private TextView tvSuccessProductName;
    private TextView tvSuccessCardInfo;
    private TextView tvSuccessAmount;
    private Button btnNextTransaction;

    private Order order;
    private double discountPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Try to get Order object first
        order = (Order) getIntent().getSerializableExtra(BleConnectActivity.EXTRA_ORDER);
        discountPercent = getIntent().getDoubleExtra(BleConnectActivity.EXTRA_DISCOUNT_PERCENT, 0.0);

        String productName;
        String cardInfo;
        int amount;

        if (order != null) {
            // New approach - calculate from Order object
            productName = order.getFormattedProductName();
            amount = order.getDiscountedPrice(discountPercent);
        } else {
            // Fallback to individual extras for backward compatibility
            productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
            if (productName == null) productName = "나이키알파플라이3 1개";
            amount = getIntent().getIntExtra("EXTRA_AMOUNT", 314100);
        }

        cardInfo = getIntent().getStringExtra("EXTRA_CARD_INFO");
        if (cardInfo == null) cardInfo = "현대백화점 카드";

        tvSuccessProductName = findViewById(R.id.tvSuccessProductName);
        tvSuccessCardInfo = findViewById(R.id.tvSuccessCardInfo);
        tvSuccessAmount = findViewById(R.id.tvSuccessAmount);
        btnNextTransaction = findViewById(R.id.btnNextTransaction);

        tvSuccessProductName.setText(productName);
        tvSuccessCardInfo.setText(cardInfo);
        tvSuccessAmount.setText(formatNumber(amount) + "원");

        btnNextTransaction.setOnClickListener(v -> {
            // Navigate to BeaconActivity (main screen)
            Intent intent = new Intent(this, BeaconActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number);
    }
}
