package com.example.apidemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mcandle.vpos.R;

import vpos.apipackage.At;
import vpos.apipackage.Beacon;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String SETTINGS_PREFS = "settingsInfo";
    private static final String BEACON_PREFS = "beaconInfo";
    private static final String SCAN_PREFS = "scanInfo";

    // Handler message types
    private static final int MSG_BEACON_QUERY_RESULT = 1;
    private static final int MSG_BEACON_START_RESULT = 2;
    private static final int MSG_BEACON_STOP_RESULT = 3;
    private static final int MSG_BEACON_CONFIG_RESULT = 4;

    // 매장 정보
    private EditText etTitle;
    private EditText etShop;
    private EditText etSalesperson;

    // BLE 설정
    private EditText etBroadcastName;

    // 버튼
    private ImageView ivBack;
    private MaterialButton btnSave;

    // 비콘 버튼
    private MaterialButton btnBeaconQuery;
    private MaterialButton btnBeaconStart;
    private MaterialButton btnBeaconStop;
    private MaterialButton btnMasterScanConfig;

    // 고급 설정 버튼
    private MaterialButton btnBeaconConfig;
    private MaterialButton btnUuidConfig;
    private MaterialButton btnSlave;

    // Handler for UI updates
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_BEACON_QUERY_RESULT:
                    handleBeaconQueryResult((Beacon) msg.obj, msg.arg1);
                    break;
                case MSG_BEACON_START_RESULT:
                    handleBeaconStartResult(msg.arg1);
                    break;
                case MSG_BEACON_STOP_RESULT:
                    handleBeaconStopResult(msg.arg1);
                    break;
                case MSG_BEACON_CONFIG_RESULT:
                    handleBeaconConfigResult(msg.arg1);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        // 매장 정보
        etTitle = findViewById(R.id.etTitle);
        etShop = findViewById(R.id.etShop);
        etSalesperson = findViewById(R.id.etSalesperson);

        // BLE 설정
        etBroadcastName = findViewById(R.id.etBroadcastName);

        // 버튼
        ivBack = findViewById(R.id.ivBack);
        btnSave = findViewById(R.id.btnSave);

        // 비콘 버튼
        btnBeaconQuery = findViewById(R.id.btn_beacon_query);
        btnBeaconStart = findViewById(R.id.btn_beacon_start);
        btnBeaconStop = findViewById(R.id.btn_beacon_stop);
        btnMasterScanConfig = findViewById(R.id.btn_master_scan_config);

        // 고급 설정 버튼
        btnBeaconConfig = findViewById(R.id.btn_beacon_config);
        btnUuidConfig = findViewById(R.id.btn_uuid_config);
        btnSlave = findViewById(R.id.btn_slave);
    }

    private void loadSettings() {
        // 매장 정보 로드
        SharedPreferences settingsSp = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        etTitle.setText(settingsSp.getString("title", "VPOS"));
        etShop.setText(settingsSp.getString("shop", "6F 나이키"));
        etSalesperson.setText(settingsSp.getString("salesperson", "한아름 (224456)"));

        // BLE 설정 로드 (broadcastName)
        SharedPreferences scanSp = getSharedPreferences(SCAN_PREFS, MODE_PRIVATE);
        etBroadcastName.setText(scanSp.getString("broadcastName", ""));
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveSettings());

        // 비콘 버튼
        btnBeaconQuery.setOnClickListener(v -> queryBeaconParams());
        btnBeaconStart.setOnClickListener(v -> startBeacon());
        btnBeaconStop.setOnClickListener(v -> stopBeacon());
        btnMasterScanConfig.setOnClickListener(v -> showScanFilterDialog());

        // 고급 설정 버튼
        btnBeaconConfig.setOnClickListener(v -> showBeaconConfigDialog());
        btnUuidConfig.setOnClickListener(v ->
            Toast.makeText(this, "UUID 설정은 준비 중입니다", Toast.LENGTH_SHORT).show());
        btnSlave.setOnClickListener(v ->
            Toast.makeText(this, "Slave 모드는 준비 중입니다", Toast.LENGTH_SHORT).show());
    }

    private void saveSettings() {
        // 매장 정보 저장
        SharedPreferences settingsSp = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor settingsEditor = settingsSp.edit();
        settingsEditor.putString("title", etTitle.getText().toString().trim());
        settingsEditor.putString("shop", etShop.getText().toString().trim());
        settingsEditor.putString("salesperson", etSalesperson.getText().toString().trim());
        settingsEditor.apply();

        // BLE 설정 저장
        SharedPreferences scanSp = getSharedPreferences(SCAN_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor scanEditor = scanSp.edit();
        scanEditor.putString("broadcastName", etBroadcastName.getText().toString().trim());
        scanEditor.apply();

        Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ==================== 비콘 기능 ====================

    private void queryBeaconParams() {
        new Thread(() -> {
            Beacon beacon = new Beacon();
            int ret = At.Lib_GetBeaconParams(beacon);

            Message msg = Message.obtain();
            msg.what = MSG_BEACON_QUERY_RESULT;
            msg.arg1 = ret;
            msg.obj = beacon;
            handler.sendMessage(msg);
        }).start();
    }

    private void handleBeaconQueryResult(Beacon beacon, int ret) {
        if (ret == 0) {
            Toast.makeText(this, "비콘 조회 성공", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "비콘 조회 실패: " + ret, Toast.LENGTH_SHORT).show();
        }
    }

    private void startBeacon() {
        new Thread(() -> {
            int ret = At.Lib_EnableBeacon(true);

            Message msg = Message.obtain();
            msg.what = MSG_BEACON_START_RESULT;
            msg.arg1 = ret;
            handler.sendMessage(msg);
        }).start();
    }

    private void handleBeaconStartResult(int ret) {
        if (ret == 0) {
            Toast.makeText(this, "비콘 시작됨", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "비콘 시작 실패: " + ret, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopBeacon() {
        new Thread(() -> {
            int ret = At.Lib_EnableBeacon(false);

            Message msg = Message.obtain();
            msg.what = MSG_BEACON_STOP_RESULT;
            msg.arg1 = ret;
            handler.sendMessage(msg);
        }).start();
    }

    private void handleBeaconStopResult(int ret) {
        if (ret == 0) {
            Toast.makeText(this, "비콘 정지됨", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "비콘 정지 실패: " + ret, Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== 스캔 필터 설정 ====================

    private void showScanFilterDialog() {
        View inputLayout = LayoutInflater.from(this).inflate(R.layout.item_scan_filter_info, null);
        EditText etMacAddress = inputLayout.findViewById(R.id.etMacAddress);
        EditText etFilterBroadcastName = inputLayout.findViewById(R.id.etBroadcastName);
        EditText etRssi = inputLayout.findViewById(R.id.etRssi);
        EditText etManufacturerId = inputLayout.findViewById(R.id.etManufacturerId);
        EditText etData = inputLayout.findViewById(R.id.etData);

        SharedPreferences sp = getSharedPreferences(SCAN_PREFS, MODE_PRIVATE);
        etMacAddress.setText(sp.getString("macAddress", ""));
        etFilterBroadcastName.setText(sp.getString("broadcastName", ""));
        etRssi.setText(sp.getString("rssi", "0"));
        etManufacturerId.setText(sp.getString("manufacturerId", ""));
        etData.setText(sp.getString("data", ""));

        new AlertDialog.Builder(this)
                .setTitle("스캔 필터 설정")
                .setView(inputLayout)
                .setCancelable(false)
                .setPositiveButton("저장", (dialogInterface, which) -> {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("macAddress", etMacAddress.getText().toString().trim());
                    editor.putString("broadcastName", etFilterBroadcastName.getText().toString().trim());
                    editor.putString("rssi", etRssi.getText().toString().trim());
                    editor.putString("manufacturerId", etManufacturerId.getText().toString().trim());
                    editor.putString("data", etData.getText().toString().trim());
                    editor.apply();

                    // BLE 설정 필드도 업데이트
                    etBroadcastName.setText(etFilterBroadcastName.getText().toString().trim());

                    Toast.makeText(this, "스캔 필터가 저장되었습니다", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // ==================== 비콘 설정 다이얼로그 ====================

    private void showBeaconConfigDialog() {
        View inputLayout = LayoutInflater.from(this).inflate(R.layout.dialog_beacon_settings, null);
        EditText etCompanyId = inputLayout.findViewById(R.id.etCompanyId);
        EditText etMajorUuid = inputLayout.findViewById(R.id.etMajorUuid);
        EditText etMinorUuid = inputLayout.findViewById(R.id.etMinorUuid);
        EditText etCustomUuid = inputLayout.findViewById(R.id.etCustomUuid);

        // 저장된 값 로드
        SharedPreferences sp = getSharedPreferences(BEACON_PREFS, MODE_PRIVATE);
        etCompanyId.setText(sp.getString("companyId", "4C00"));
        etMajorUuid.setText(sp.getString("majorUuid", "0708"));
        etMinorUuid.setText(sp.getString("minorUuid", "0506"));
        etCustomUuid.setText(sp.getString("customUuid", "0112233445566778899AABBCCDDEEFF0"));

        new AlertDialog.Builder(this)
                .setTitle("비콘 설정")
                .setView(inputLayout)
                .setCancelable(false)
                .setPositiveButton("저장", (dialogInterface, which) -> {
                    String companyId = etCompanyId.getText().toString().trim();
                    String majorUuid = etMajorUuid.getText().toString().trim();
                    String minorUuid = etMinorUuid.getText().toString().trim();
                    String customUuid = etCustomUuid.getText().toString().trim();

                    // SharedPreferences에 저장
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("companyId", companyId);
                    editor.putString("majorUuid", majorUuid);
                    editor.putString("minorUuid", minorUuid);
                    editor.putString("customUuid", customUuid);
                    editor.apply();

                    // 디바이스에 설정
                    if (!TextUtils.isEmpty(companyId) && !TextUtils.isEmpty(majorUuid)
                            && !TextUtils.isEmpty(minorUuid) && !TextUtils.isEmpty(customUuid)) {
                        new Thread(() -> {
                            Beacon beacon = new Beacon(companyId, majorUuid, minorUuid, customUuid);
                            int ret = At.Lib_SetBeaconParams(beacon);

                            Message msg = Message.obtain();
                            msg.what = MSG_BEACON_CONFIG_RESULT;
                            msg.arg1 = ret;
                            handler.sendMessage(msg);
                        }).start();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void handleBeaconConfigResult(int ret) {
        if (ret == 0) {
            Toast.makeText(this, "비콘 설정 완료", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "비콘 설정 실패: " + ret, Toast.LENGTH_SHORT).show();
        }
    }
}
