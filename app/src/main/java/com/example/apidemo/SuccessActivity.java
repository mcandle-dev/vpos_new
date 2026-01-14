package com.example.apidemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

import com.mcandle.vpos.R;

public class SuccessActivity extends AppCompatActivity {

    private TextView tvSuccessProductName;
    private TextView tvSuccessAmount;
    private Button btnNextTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        String productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        if (productName == null) productName = "상품";

        int amount = getIntent().getIntExtra("EXTRA_AMOUNT", 0);

        tvSuccessProductName = findViewById(R.id.tvSuccessProductName);
        tvSuccessAmount = findViewById(R.id.tvSuccessAmount);
        btnNextTransaction = findViewById(R.id.btnNextTransaction);

        tvSuccessProductName.setText(productName);
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
