package com.example.apidemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.apidemo.adapter.DeviceAdapter;
import com.example.apidemo.ble.BleConnection;
import com.example.apidemo.ble.Device;
import com.example.apidemo.ble.DividerItemDecoration;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.bartwell.exfilepicker.utils.Utils;
import vpos.apipackage.At;
import vpos.apipackage.Beacon;
import vpos.apipackage.Com;

import com.mcandle.vpos.R;

public class BeaconActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RECORD_PROMPT_MSG = 0x06;
    private static final int SCAN_DATA_PROMPT_MSG = 0x08;
    private static final int STOP_SCAN_DATA_PROMPT_MSG = 0x10;
    private static final String SETTINGS_PREFS = "settingsInfo";

    private boolean mStartFlag = false;
    private boolean mEnableFlag = true;
    private String customUUID = "0x0112233445566778899AABBCCDDEEFF0";

    private boolean mMasterFlag = false;
    public  boolean startScan =false;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;

    // Header UI elements
    private TextView tvHeaderLogo;
    private TextView tvHeaderStaff;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        // Hide ActionBar - using custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize BLE connection
        // bleConnection = new BleConnection();

        initView();
        initData();
        initEvent();
    }

    private void initView() {
        // Header
        tvHeaderLogo = findViewById(R.id.tvHeaderLogo);
        tvHeaderStaff = findViewById(R.id.tvHeaderStaff);

        // RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        deviceAdapter = new DeviceAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        recyclerView.setAdapter(deviceAdapter);

        // Set click listener for device list - navigate to BleConnectActivity
        deviceAdapter.setOnDeviceClickListener(device -> {
            navigateToBleConnect(device);
        });

        // Settings icon click listener
        ImageView ivSettings = findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(v -> showSettingsDialog());

        // Load saved settings to header
        loadSettingsToHeader();
    }

    private void initData() {
        // Initialize with empty device list
        List<Device> newDeviceList = new ArrayList<>();
        deviceAdapter.setDeviceList(newDeviceList);
    }

    private void initEvent() {
        // ActionBar is hidden, using custom header instead
        findViewById(R.id.btn_beacon_query).setOnClickListener(this);
        findViewById(R.id.btn_beacon_start).setOnClickListener(this);
        findViewById(R.id.btn_beacon_stop).setOnClickListener(this);
        findViewById(R.id.btn_master_scan).setOnClickListener(this);
        findViewById(R.id.btn_master_scanStop).setOnClickListener(this);
        findViewById(R.id.btn_master_scan_config).setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if (!mEnableFlag) {
            new AlertDialog.Builder(this)
                    .setTitle("Disable Beacon?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialogInterface, which) -> {
                        super.finish();
                    })
                    .setNegativeButton("No", (dialogInterface, which) -> {
                        new Thread(() -> {
                            if (mMasterFlag) {
                                At.Lib_EnableMaster(true);
                            } else {
                                At.Lib_EnableBeacon(true);
                            }
                        }).start();
                        super.finish();
                    })
                    .show();
            return;
        }
        if(startScan) {
            startScan = false;
            At.Lib_AtStopScan();
        }
        super.finish();
    }

    public void SendPromptMsg(String strInfo) {
        // Log instead of displaying in removed cardMessage
        if (strInfo != null && !strInfo.isEmpty()) {
            Log.d("BeaconActivity", strInfo);
        }
    }

    public void SendPromptScanMsg(String strInfo) {
        Message msg = new Message();
//        Log.e("TAG", "debug crash position:echo11" );
        msg.what = SCAN_DATA_PROMPT_MSG;
        Bundle b = new Bundle();
        b.putString("MSG", strInfo);
        msg.setData(b);
//        Log.e("TAG", "debug crash position:echo11" +strInfo);
        promptHandler.sendMessage(msg);
//        Log.e("TAG", "debug crash position:echo13" );
    }

    public void SendPromptScanStopMsg(String strInfo) {
        Message msg = new Message();
        msg.what = STOP_SCAN_DATA_PROMPT_MSG;
        Bundle b = new Bundle();
        b.putString("MSG", strInfo);
        msg.setData(b);
        promptHandler.sendMessage(msg);

    }
    @SuppressLint("HandlerLeak")
    private Handler promptHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String strInfo = b.getString("MSG");

            switch (msg.what) {
                case RECORD_PROMPT_MSG:
                    // Log message (cardMessage removed)
                    if (strInfo != null && !strInfo.isEmpty()) {
                        Log.d("BeaconActivity", strInfo);
                    }
                    break;
                case STOP_SCAN_DATA_PROMPT_MSG:
                    if (strInfo != null && !strInfo.isEmpty()) {
                        Log.d("BeaconActivity", strInfo);
                    }
                    deviceAdapter.clearDeviceList();
                    promptHandler.removeCallbacksAndMessages(null);
                    break;
                case SCAN_DATA_PROMPT_MSG:
//                    Log.e("TAG", "debug crash position:echo1" );
                    if (strInfo.equals("")||strInfo.length()<6) {
//                        tv_msg.setText("");
//                        tv_msg.scrollTo(0, 0);
//                        Log.e("TAG", "debug crash position:echo2" );
                        deviceAdapter.removeDisappearDevice();
                    } else {
//                        tv_msg.append(strInfo);
//                        tv_msg.setText(strInfo);
                        try {
                            Log.e("TAG", "handleMessage: "+ strInfo);
                            JSONArray jsonArray = new JSONArray(strInfo);
//                            Log.e("TAG", "debug crash position:echo3" );
                            List<Device> newDeviceList = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject deviceJson = jsonArray.getJSONObject(i);

                                if (!deviceJson.has("MAC") || !deviceJson.has("RSSI")) {
                                    Log.w("JSON Parse", "Missing required fields in device: "+deviceJson);
                                    continue;
                                }
                                // ��ȫ����ת��
                                String mac = deviceJson.getString("MAC");
                                int rssi = deviceJson.optInt("RSSI", -999); // ʹ��optInt�ṩĬ��ֵ

                                // ��Χ��Ч��У��
                                if(rssi < -120 || rssi > 20) {
                                    Log.w("RSSI Range", "Invalid RSSI value: "+rssi+" for MAC: "+mac);
                                    continue;
                                }

                                // Ƕ�׶���ȫ����
                                String deviceName = null;
                                String uuid = null;
                                JSONObject advObj = deviceJson.optJSONObject("ADV");
                                JSONObject rspObj = deviceJson.optJSONObject("RSP");

                                if(advObj != null) {
                                    deviceName = advObj.optString("Device Name", null);
                                    uuid = advObj.optString("Service UUIDs", null);
                                }
                                if(rspObj != null && uuid == null) {
                                    uuid = rspObj.optString("Service UUIDs", null);
                                }
                                if(rspObj != null && deviceName == null) {
                                    deviceName = rspObj.optString("Device Name", null);
                                }


                                // ʱ�����ȫ��ȡ
                                long timestamp = deviceJson.optLong("Timestamp", System.currentTimeMillis());

                                deviceAdapter.updateDevice(new Device(
                                        deviceName,
                                        mac,
                                        rssi,
                                        uuid,
                                        timestamp
                                ));

                            }

                        } catch (JSONException e) {
                            Log.e("TAG", "Handler promptHandler 0000: JSONException"+e.getMessage() );
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        if(len%2==1)
            len--;
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    private static String bytesToHex(byte[] bytes,int len) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<len;i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
    public static JSONObject parseAdvertisementData(byte[] advertisementData) throws JSONException {
//        Map<String, String> parsedData = new HashMap<>();
//        byte[] advertisementData =new byte[advertiseData.length()/2];
        JSONObject parsedData = new JSONObject();
        int offset = 0;
        while (offset < advertisementData.length) {
            int length = advertisementData[offset++] & 0xFF;
            if (length == 0) break;

            int type = advertisementData[offset] & 0xFF;
            offset++;

            byte[] data = new byte[length - 1];
            if(length-1>advertisementData.length-offset)//data format issue.
            {
                return null;
            }
            System.arraycopy(advertisementData, offset, data, 0, length - 1);
            offset += length - 1;

            switch (type) {
                case 0x01: // Flags
                    parsedData.put("Flags", bytesToHex(data));
                    break;
                case 0x02: // Incomplete List of 16-bit Service Class UUIDs
                case 0x03: // Complete List of 16-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x04: // Incomplete List of 32-bit Service Class UUIDs
                case 0x05: // Complete List of 32-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x06: // Incomplete List of 128-bit Service Class UUIDs
                case 0x07: // Complete List of 128-bit Service Class UUIDs
                    parsedData.put("Service UUIDs", bytesToHex(data));
                    break;
                case 0x08: // Shortened Local Name
                case 0x09: // Complete Local Name
                    parsedData.put("Device Name", new String(data));
                    break;
                case 0x0A: // Complete Local Name
//                    byte [] tx_power=hexStringToByteArray(new String(data));
                    parsedData.put("TX Power Level", data[0]);
                    break;
                case 0xFF: // Manufacturer Specific Data
                    parsedData.put("Manufacturer Data", bytesToHex(data));
                    break;
                default:
                    parsedData.put("Unknown Data (" + type + ")", bytesToHex(data));
                    break;
            }
        }

        return parsedData;
    }

private static JSONObject parsePayload(String payload) {
    JSONObject result = new JSONObject();
    int index = 0;

    while (index < payload.length()) {
        // ��������Ƿ�Խ��
        if (index + 2 > payload.length()) {
            break;
        }
        int length = Integer.parseInt(payload.substring(index, index + 2), 16);
        index += 2;

        // ��������Ƿ�Խ��
        if (index + 2 > payload.length()) {
            break;
        }
        int type = Integer.parseInt(payload.substring(index, index + 2), 16);
        index += 2;

        // ��������Ƿ�Խ��
        if (index + length * 2 > payload.length()) {
            break;
        }
        String data = payload.substring(index, index + length * 2);
        index += length * 2;

        try {
            result.put("Type " + type, data);
        } catch (JSONException e) {
            Log.e("TAG", "parsePayload:Type "+e.getMessage() );
//            throw new RuntimeException(e);
            return null;
        }
    }

    return result;
}

    Runnable  recvScanData = new Runnable (){
        byte[] recvData =new byte[2048];
        int[] recvDataLen =new int[2];
        String lineLeft="";
        public void run() {
            while(startScan)
            {
                int ret = At.Lib_ComRecvAT(recvData, recvDataLen, 20, 2048);
                Log.e("TAG", "runLib_ComRecvAT: recvDataLen"+recvDataLen[0] );
                Log.e("TAG", "Lib_ComRecvAT recvData: "+bytesToHex(recvData,recvDataLen[0]));
                Map<String, JSONObject> deviceMap = new HashMap<>();
                boolean startProcessing = false;
                // String buff= lineLeft+new String(recvData);
                String buff= lineLeft+new String(recvData, 0, recvDataLen[0]);
                // String []data=buff.split("\r\n|\r|\n");
                String []data=buff.split("\\r\\n|\\r|\\n", -1); // ����λ�������ַ���
                //Log.e("TAG", "debug crash position:echo21" );
                int lineCount=data.length;
                // if(lineCount>0)//each time response data left last line ,for maybe data not recv all.
                //     lineLeft = data[lineCount-1];
                // else
                //     lineLeft="";
                // �������һ��δ�������
                lineLeft = (data.length > 0) ? data[data.length-1] : "";
                //for (String line : data)
                for (int i=0;i<lineCount-1;i++)
                {
                    String line =data[i];
//                    Log.e("TAG", "debug crash position:echo22" );
                    if (line.startsWith("MAC:")) {
                        startProcessing = true;
                        String[] parts = line.split(",",3);
                        if(parts.length < 3) {
                            continue;
                        }
                        
                        String mac = parts[0].split(":",2)[1].trim();
                        String rssi = parts[1].split(":")[1].trim();
                        int irssi =0;
                        try {
                            irssi = Integer.parseInt(rssi); // ��֤ RSSI �Ƿ�Ϊ��Ч����
                        } catch (NumberFormatException e) {
                            Log.e("TAG", "Invalid RSSI value: " + rssi);
                            continue;
                        }
                        String payload = parts[2].split(":",2)[1].trim();
                        if((payload.length()>62)||(payload.length()%2!=0))
                            continue;
//                        Log.e("TAG", "debug crash position:echo20" );
                        JSONObject device;
                        if (deviceMap.containsKey(mac)) {
                            device = deviceMap.get(mac);
                        } else {
                            device = new JSONObject();
                            try {
                                device.put("MAC", mac);
                            } catch (JSONException e) {
                                Log.e("TAG", "Handler runLib_ComRecvAT mac 0000: JSONException"+e );
                                //throw new RuntimeException(e);
								 continue;
                            }
                            deviceMap.put(mac, device);
                        }
//                        Log.e("TAG", "debug crash position:echo19" );
                        if (parts[2].startsWith("RSP")) {

                            try {
                                assert device != null;
                                device.put("RSP_org", payload);
                                device.put("RSP", parseAdvertisementData(hexStringToByteArray(payload)));
                            } catch (JSONException e) {
                                Log.e("TAG", "Runnable 444: JSONException"+e );
//                                throw new RuntimeException(e);
                                continue;
                            }

                        } else if (parts[2].startsWith("ADV")) {
                            //device.put("ADV", parsePayload(payload));
                            try {
                                assert device != null;
                                device.put("ADV_org", payload);
                                device.put("ADV", parseAdvertisementData(hexStringToByteArray(payload)));
                            } catch (JSONException e) {
                                Log.e("TAG", "Runnable 333: JSONException"+e );
//                                throw new RuntimeException(e);
                                continue;
                            }
                        }
                        //Log.e("TAG", "debug crash position:echo18" );
                        try {
                            assert device != null;
                           // Log.e("TAG", "debug crash position:echo18"+rssi );
                            device.put("RSSI", irssi);
                        } catch (JSONException e) {
                            Log.e("TAG", "Runnable 222: JSONException"+e.getMessage() );
//                            throw new RuntimeException(e);
                            continue;
                        }
//                        Log.e("TAG", "debug crash position:echo17" );
                        // ���ʱ����ֶ�
                        try {
//                                long curr_time=System.currentTimeMillis();
                            device.put("Timestamp", System.currentTimeMillis());
                        } catch (JSONException e) {
                            //Log.e("TAG", "Runnable 000: JSONException"+e );
//                            throw new RuntimeException(e);
                            continue;
                        }
//                        Log.e("TAG", "debug crash position:echo16" );
                    } else if (startProcessing) {
                        // ����Ѿ���ʼ����MAC���ݣ���������MAC��ͷ�����ݣ�������
                        continue;
                    }
//                    Log.e("TAG", "debug crash position:echo14---"+);

                }

                // ������ת��ΪJSON����

                JSONArray jsonArray = new JSONArray(deviceMap.values());
                try {
//                        SendPromptMsg("" + jsonArray.toString(4));
//                    Log.e("TAG", "debug crash position:echo14" );
                    //if(jsonArray.)
                    SendPromptScanMsg("" + jsonArray.toString(4));
//                    Log.e("TAG", "debug crash position:echo15" );
                } catch (JSONException e) {
                    Log.e("TAG", "Runnable 111: JSONException"+ e.getMessage() );
//                    throw new RuntimeException(e);
                }
            };
        }

    };
    private boolean isValidMacAddress(String macAddress) {
        // �򵥵�MAC��ַ��ʽ��֤�������Ը���ʵ����������޸�
        return macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    private boolean isValidManufacturerId(String manufacturerId) {
        // �򵥵�ManufacturerId��ʽ��֤�������Ը���ʵ����������޸�
        return manufacturerId.matches("^[0-9A-Fa-f]+$");
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        if (R.id.btn_beacon_config == viewId) {
            if (mStartFlag) {
                Log.i("unique Start", "start---------->flag=" + mStartFlag);
                return;
            }
            mStartFlag = true;
            SendPromptMsg("");

            View inputLayout = LayoutInflater.from(this).inflate(R.layout.item_beacon_info, null);
            EditText etCompanyId = inputLayout.findViewById(R.id.etCompanyId);
            EditText etMajorUuid = inputLayout.findViewById(R.id.etMajorUuid);
            EditText etMinorUuid = inputLayout.findViewById(R.id.etMinorUuid);
            EditText etCustomUuid = inputLayout.findViewById(R.id.etCustomUuid);

            SharedPreferences sp = getSharedPreferences("beaconInfo", MODE_PRIVATE);
            etCompanyId.setText(sp.getString("companyId", "4C00"));
            etMajorUuid.setText(sp.getString("majorUuid", "0708"));
            etMinorUuid.setText(sp.getString("minorUuid", "0506"));
            etCustomUuid.setText(sp.getString("customUuid", "0112233445566778899AABBCCDDEEFF0"));

            new AlertDialog.Builder(this)
                    .setTitle("Config Beacon")
                    .setView(inputLayout)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialogInterface, which) -> {
                        String companyId = etCompanyId.getText().toString().trim();
                        String majorUuid = etMajorUuid.getText().toString().trim();
                        String minorUuid = etMinorUuid.getText().toString().trim();
                        String customUuid = etCustomUuid.getText().toString().trim();

                        if (TextUtils.isEmpty(companyId)
                                || TextUtils.isEmpty(majorUuid)
                                || TextUtils.isEmpty(minorUuid)
                                || TextUtils.isEmpty(customUuid)) {
                            SendPromptMsg("Empty Field!\n");
                            mStartFlag = false;
                            return;
                        }

                        new Thread(() -> {
                            Beacon beacon = new Beacon(companyId, majorUuid, minorUuid, customUuid);
                            int ret = At.Lib_SetBeaconParams(beacon);
                            if (ret == 0) {
                                SendPromptMsg("Config beacon succeeded!\n"
                                        + "Company ID: 0x" + beacon.companyId + "\n"
                                        + "Major: 0x" + beacon.major + "\n"
                                        + "Minor: 0x" + beacon.minor + "\n"
                                        + "Custom UUID: 0x" + beacon.customUuid + "\n");

                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("companyId", companyId);
                                editor.putString("majorUuid", majorUuid);
                                editor.putString("minorUuid", minorUuid);
                                editor.putString("customUuid", customUuid);
                                editor.apply();
                            } else {
                                SendPromptMsg("Config beacon failed, return: " + ret + "\n");
                            }
                            mStartFlag = false;
                        }).start();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
                        SendPromptMsg("Cancel Config Beacon.\n");
                        mStartFlag = false;
                    })
                    .show();
            return;
        }
        else if (R.id.btn_master_scan_config == viewId) {
            SendPromptMsg("SCAN CONFIG\n");
            int ret = 0;
            String[] mac = new String[1];
            startScan=true;

            View inputLayout = LayoutInflater.from(this).inflate(R.layout.item_scan_filter_info, null);
            EditText etMacAddress = inputLayout.findViewById(R.id.etMacAddress);
            EditText etBroadcastName = inputLayout.findViewById(R.id.etBroadcastName);
            EditText etRssi = inputLayout.findViewById(R.id.etRssi);
            EditText etManufacturerId = inputLayout.findViewById(R.id.etManufacturerId);
            EditText etData = inputLayout.findViewById(R.id.etData);

            SharedPreferences sp = getSharedPreferences("scanInfo", MODE_PRIVATE);
            etMacAddress.setText(sp.getString("macAddress", ""));
            etBroadcastName.setText(sp.getString("broadcastName", ""));
            etRssi.setText(sp.getString("rssi", "0"));
            etManufacturerId.setText(sp.getString("manufacturerId", ""));
            etData.setText(sp.getString("data", ""));

            new AlertDialog.Builder(this)
                    .setTitle("Config Scan Filter")
                    .setView(inputLayout)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialogInterface, which) -> {
                        String macAddress = etMacAddress.getText().toString().trim();
                        String broadcastName = etBroadcastName.getText().toString().trim();
                        String rssi = etRssi.getText().toString().trim();
                        String manufacturerId = etManufacturerId.getText().toString().trim();
                        String data = etData.getText().toString().trim();

                        if (!TextUtils.isEmpty(macAddress)&&!isValidMacAddress(macAddress)) {
                            SendPromptMsg("Invalid MAC Address!\n");
//                            mStartFlag = false;
                            return;
                        }

//                        if (TextUtils.isEmpty(broadcastName)) {
//                            SendPromptMsg("Broadcast Name is required!\n");
//                            mStartFlag = false;
//                            return;
//                        }

                        if (TextUtils.isEmpty(rssi)) {
                            SendPromptMsg("RSSI is required!\n");
//                            mStartFlag = false;

//                            return;
                        }

                        if (TextUtils.isEmpty(manufacturerId) || !isValidManufacturerId(manufacturerId)) {
                            SendPromptMsg("Invalid ManufacturerId!\n");
//                            mStartFlag = false;
//                            return;
                        }

                        // �����ﴦ����������ݣ����籣�浽SharedPreferences�������������
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("macAddress", macAddress);
                        editor.putString("broadcastName", broadcastName);
                        editor.putString("rssi", rssi);
                        editor.putString("manufacturerId", manufacturerId);
                        editor.putString("data", data);
                        editor.apply();

                        // ����ִ����������
                    })
                    .setNegativeButton("Cancel", null)
                    .show();



//

            if (ret == 0) {
                SendPromptMsg("NEW DEVICE DISCOVERED: " + mac[0] + "\n");
            } else {
                SendPromptMsg("ERROR WHILE SCANNING, RET = " + ret + "\n");
            }
        }else if (R.id.btn_master_scan == viewId) {
            SendPromptMsg("SCANNING\n");
            int ret = 0;
            ret = At.Lib_EnableMaster(true);

            // Configure BLE services before scanning
            Log.d("VPOS", "Configuring BLE services before scanning...");
//            boolean serviceConfigured = configureBleServices();
//            if (!serviceConfigured) {
//                Log.e("VPOS", "Failed to configure BLE services");
//                SendPromptMsg("ERROR CONFIGURING BLE SERVICES\n");
//                return;
//            }
//            Log.d("VPOS", "BLE services configured successfully");

            String[] mac = new String[1];
            startScan=true;
            SharedPreferences sp = getSharedPreferences("scanInfo", MODE_PRIVATE);
            Log.e("TAG", "onClick: "+"macAddress"+sp.getString("macAddress", ""));
            Log.e("TAG", "onClick: "+"broadcastName"+sp.getString("broadcastName", ""));
            Log.e("TAG", "onClick: "+"rssi"+sp.getString("rssi", ""));
            Log.e("TAG", "onClick: "+"manufacturerId"+sp.getString("manufacturerId", ""));
            Log.e("TAG", "onClick: "+"data"+sp.getString("data", ""));
            ret = At.Lib_AtStartNewScan(sp.getString("macAddress", ""), sp.getString("broadcastName", ""),
                    -Integer.parseInt(sp.getString("rssi", "0")),sp.getString("manufacturerId", ""),sp.getString("data", ""));
            if(ret==0) {
                // recvScanData.start();
                new Thread(recvScanData).start();
            }

            if (ret == 0) {
                SendPromptMsg("NEW DEVICE DISCOVERED: " + mac[0] + "\n");
            } else {
                SendPromptMsg("ERROR WHILE SCANNING, RET = " + ret + "\n");
            }
        }


        new Thread() {

            public void run() {
                if (mStartFlag) {
                    Log.i("unique Start", "start---------->flag=" + mStartFlag);
                    return;
                }
                mStartFlag = true;
                SendPromptMsg("");

                if (R.id.btn_beacon_query == viewId) {
                    Beacon beacon = new Beacon();
                    int ret = At.Lib_GetBeaconParams(beacon);
                    if (ret == 0) {
                        SendPromptMsg("Query beacon succeeded!\n"
                                + "Company ID: " + beacon.companyId + "\n"
                                + "Major: " + beacon.major + "\n"
                                + "Minor: " + beacon.minor + "\n"
                                + "Custom UUID: " + beacon.customUuid + "\n");
                    } else {
                        SendPromptMsg("Query beacon failed, return: " + ret + "\n");
                    }
                }

                else if (R.id.btn_beacon_start == viewId) {

                    int ret = 0;
                    if (mMasterFlag) {
                        ret = At.Lib_EnableMaster(true);
                    } else {
                        ret = At.Lib_EnableBeacon(true);
                    }

                    if (ret == 0) {
                        mEnableFlag = true;
                        if (mMasterFlag) {
                            SendPromptMsg("Start master succeeded!\n");
                        } else {
                            SendPromptMsg("Start beacon succeeded!\n");
                        }
                        SendPromptMsg("Note: Effective immediately; Power-off preservation.\n");
                    } else {
                        SendPromptMsg("Start beacon failed, return: " + ret + "\n");
                    }
                }
                else if (R.id.btn_beacon_stop == viewId) {
                    int ret = 0;
                    if (mMasterFlag) {
                        ret = At.Lib_EnableMaster(false);
                    } else {
                        ret = At.Lib_EnableBeacon(false);
                    }
                    if (ret == 0) {
                        mEnableFlag = false;
                        if (mMasterFlag) {
                            SendPromptMsg("Stop master succeeded!\n");
                        } else {
                            SendPromptMsg("Stop beacon succeeded!\n");
                        }
                        SendPromptMsg("Note: Effective immediately; Power-off preservation.\n");
                    } else {
                        SendPromptMsg("Stop beacon failed, return: " + ret + "\n");
                    }
                } else if (R.id.btn_master_scanStop == viewId) {
                    SendPromptMsg("SCANNING stop\n");
                    int ret = 0;
                    startScan=false;

//                    recvScanData.interrupt();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.e("TAG", "R.id.btn_beacon_start 0000: JSONException"+e );
                        throw new RuntimeException(e);
                    }
                    ret = At.Lib_AtStopScan();

                    if (ret == 0) {
                        SendPromptScanStopMsg("STOP SCAN SUCCESS: " + "\n");
                    } else {
                        SendPromptMsg("ERROR WHILE STOP SCANG, RET = " + ret + "\n");
                    }


                } else if (R.id.btn_slave == viewId) {
                    SendPromptMsg("Configuring BLE services for slave role...\n");
                    boolean serviceConfigured = configureBleServices_role0();
                    if (serviceConfigured) {
                        SendPromptMsg("BLE services configured successfully for slave role\n");
                    } else {
                        SendPromptMsg("Failed to configure BLE services for slave role\n");
                    }
                } else if (R.id.btn_uuid_config == viewId) {
                    SendPromptMsg("Configuring BLE services with UUID config...\n");
                    boolean serviceConfigured = configureBleServices();
                    if (serviceConfigured) {
                        SendPromptMsg("BLE services configured successfully with UUID config\n");
                    } else {
                        SendPromptMsg("Failed to configure BLE services with UUID config\n");
                    }
                }

                mStartFlag = false;
            }
        }.start();
    }

    /**
     * Check and configure BLE services
     * @return true if configuration is correct or successfully updated
     */
    private boolean configureBleServices() {
        Log.d("VPOS", "=== BLE Service Configuration ===");
        
        try {

            BleConnection bleConnection = new BleConnection();
            int iret= bleConnection.atCmdSendrcv("AT+ROLE?\r\n","",1000,256,"AT+ROLE=1");

            if(iret!=0){
                Log.e("VPOS", " role!=1 need update to role=1 ");
                 iret= bleConnection.atCmdSendrcv("AT+ROLE=1\r\n","",1000,256,"ok");
                if(iret==0){
                    Log.e("VPOS", " set role=1 success change to master mode ");
                    Log.d("VPOS", "change  mode need restart");
                    iret= bleConnection.atCmdSendrcv("AT+RESTART\r\n","",3000,256,"OK");
                    if(iret==0){
                        Log.e("VPOS", "set RESTART success");
                        //need entry +++ mode
                        Thread.sleep(1000);
                        Log.e("VPOS", "entry +++");
                        iret= bleConnection.atCmdSendrcv("+++","",4000,256,"OK");
                        if(iret==0){
                            Log.e("VPOS", "set RESTART success");
                            //return true;
                        }
                    }
                }
                else {
                    Log.e("VPOS", "set  role=1 failed ");
                }
                //return true;
            }
            // check uuid is disable
            iret= bleConnection.atCmdSendrcv("AT+UUID_SCAN?\r\n","",1000,256,"AT+UUID_SCAN=0");
            if(iret!=0){
                Log.e("VPOS", " UUID_SCAN should be disable ");
                iret= bleConnection.atCmdSendrcv("AT+UUID_SCAN=0\r\n","",1000,256,"ok");
                if(iret==0){
                    Log.e("VPOS", " set UID_SCAN=0 success  ");
                }
                else {
                    Log.e("VPOS", "set  UID_SCAN=0 failed ");
                }
                //return true;
            }
            String rspMservice="";
            // check uuid is disable
            iret= bleConnection.atCmdSendrcv("AT+MSERVICE?\r\n",rspMservice,1000,256,"MSERVICE=1,FFF0,FFF0,FFF2,FFF1");
            if(iret!=0){
                Log.e("VPOS", " UUID_SCAN should be disable ");
                iret= bleConnection.atCmdSendrcv("AT+MSERVICE=1,FFF0,FFF0,FFF2,FFF1\r\n","",3000,256,"ok");
                if(iret==0){
                    Log.e("VPOS", " set MSERVICE=1,FFF0,FFF0,FFF2,FFF1 success  ");
                }
                else {
                    Log.e("VPOS", "set  MSERVICE=1,FFF0,FFF0,FFF2,FFF1 failed ");
                    return false;
                }
                //return true;
            }



//            configureBleServices_role1();
//            Log.d("VPOS", "\n[Step 4-1.5] Enabling UUID Scan (for auto UUID discovery)...");
//            String uuidScanCmd = "AT+UUID_SCAN=0\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + uuidScanCmd.trim());
//            int ret = At.Lib_ComSend(uuidScanCmd.getBytes(), uuidScanCmd.length());
//            Log.d("VPOS", "[AT CMD] Lib_ComSend returned: " + ret);
//
//            if (ret != 0) {
//                Log.w("VPOS", "Failed to send UUID_SCAN command, ret: " + ret);
//                // Don't fail - continue with connection
//            } else {
//                byte[] uuidScanResponse = new byte[128];
//                int[] uuidScanLen = new int[1];
//                ret = At.Lib_ComRecvAT(uuidScanResponse, uuidScanLen, 2000, 128);
//                String uuidScanResponseStr = new String(uuidScanResponse, 0, uuidScanLen[0]);
//                Log.i("VPOS", "[AT RSP] <<< " + uuidScanResponseStr.replace("\r\n", "\\r\\n"));
//
//                if (uuidScanResponseStr.contains("OK")) {
//                    Log.d("VPOS", "✓ UUID Scan disabled - ");
//                } else {
//                    Log.w("VPOS", "UUID scan disabled failed: " + uuidScanResponseStr);
//                }
//            }
//            // Check current service configuration
//            Log.d("VPOS", "Checking current service configuration with AT+SERVICE?");
//            String serviceCmd = "AT+MSERVICE?\r\n";
////            String serviceCmd = "AT+ROLE=0\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + serviceCmd.trim());
//            ret = At.Lib_ComSend(serviceCmd.getBytes(), serviceCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send AT+MSERVICE? command");
//                return false;
//            }
//
//            byte[] serviceResponse = new byte[256];
//            int[] serviceLen = new int[1];
//            ret = At.Lib_ComRecvAT(serviceResponse, serviceLen, 2000, 256);
//            String serviceResponseStr = new String(serviceResponse, 0, serviceLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
////            if(serviceResponseStr.contains("OK")) {
////                Log.d("VPOS", "Restarting module with AT+RESTART");
////                String restartCmd = "AT+RESTART\r\n";
////                Log.i("VPOS", "[AT CMD] >>> " + restartCmd.trim());
////                ret = At.Lib_ComSend(restartCmd.getBytes(), restartCmd.length());
////
////                if (ret != 0) {
////                    Log.e("VPOS", "Failed to send AT+RESTART command");
////                    return false;
////                }
////
////                // Wait for restart
////                Log.d("VPOS", "Waiting for module to restart...");
////                Thread.sleep(3000); // Wait 3 seconds for restart
////                byte[] restartrsp = new byte[256];
////                int[] restartrspLen = new int[1];
////                ret = At.Lib_ComRecvAT(serviceResponse, serviceLen, 2000, 256);
////                String restartrspStr = new String(serviceResponse, 0, serviceLen[0]);
////                Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
////                Log.d("VPOS", "=== BLE Service Configuration Completed ===");
////                return false;
////            }
//            // Check if services are already configured correctly
//            boolean hasCorrectServices =
//                                        serviceResponseStr.contains("FFF0") &&
//                                        serviceResponseStr.contains("FFF2") &&
//                                        serviceResponseStr.contains("FFF1");
//
//            if (hasCorrectServices) {
//                Log.d("VPOS", "✓ Services already configured correctly: FFF0,FFF1,FFF2,FFF3");
//                return true;
//            }
//
//            // Configure services
//            Log.d("VPOS", "Configuring services with AT+MSERVICE=1,FFF0,FFF1,FFF2,FFF3");
//            String mserviceCmd = "AT+MSERVICE=1,FFF0,FFF0,FFF2,FFF1\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + mserviceCmd.trim());
//            ret = At.Lib_ComSend(mserviceCmd.getBytes(), mserviceCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send AT+MSERVICE command");
//                return false;
//            }
//
//            byte[] mserviceResponse = new byte[256];
//            int[] mserviceLen = new int[1];
//            ret = At.Lib_ComRecvAT(mserviceResponse, mserviceLen, 2000, 256);
//            String mserviceResponseStr = new String(mserviceResponse, 0, mserviceLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + mserviceResponseStr.replace("\r\n", "\\r\\n"));
//
//            if (!mserviceResponseStr.contains("OK")) {
//                Log.e("VPOS", "Failed to configure services");
//                return false;
//            }
//
//            // Restart module
//            Log.d("VPOS", "Restarting module with AT+RESTART");
//            String restartCmd = "AT+RESTART\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + restartCmd.trim());
//            ret = At.Lib_ComSend(restartCmd.getBytes(), restartCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send AT+RESTART command");
//                return false;
//            }
//
//            // Wait for restart
//            Log.d("VPOS", "Waiting for module to restart...");
//            Thread.sleep(1000); // Wait 3 seconds for restart
//            byte[] restartrsp = new byte[256];
//            int[] restartrspLen = new int[1];
//            ret = At.Lib_ComRecvAT(restartrsp, restartrspLen, 2000, 256);
//            String restartrspStr = new String(restartrsp, 0, restartrspLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + restartrspStr.replace("\r\n", "\\r\\n"));
////            Log.d("VPOS", "=== BLE Service Configuration Completed ===");
//            Log.d("VPOS", "=== BLE Service Configuration Completed ===");
//            //+++
//            String entryCmd = "+++";
//            Log.i("VPOS", "[AT CMD] >>> " + entryCmd.trim());
//            ret = At.Lib_ComSend(entryCmd.getBytes(), entryCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send +++ command");
//                return false;
//            }
//            byte[] rspEntry = new byte[256];
//            int[] rspEntryLen = new int[1];
//            ret = At.Lib_ComRecvAT(rspEntry, rspEntryLen, 2000, 256);
//            String rspEntryStr = new String(rspEntry, 0, rspEntryLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + rspEntryStr.replace("\r\n", "\\r\\n"));
            return true;
            
        } catch (Exception e) {
            Log.e("VPOS", "Service configuration error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
//    private boolean configureBleServices_role1() {
//        Log.d("VPOS", "=== BLE Service Configuration ===");
//
//        try {
//            // Check current service configuration
//            Log.d("VPOS", "Checking current service configuration with AT+SERVICE?");
////            String serviceCmd = "AT+MSERVICE?\r\n";
//            String serviceCmd = "AT+ROLE=1\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + serviceCmd.trim());
//            int ret = At.Lib_ComSend(serviceCmd.getBytes(), serviceCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send AT+SERVICE? command");
//                return false;
//            }
//
//            byte[] serviceResponse = new byte[256];
//            int[] serviceLen = new int[1];
//            ret = At.Lib_ComRecvAT(serviceResponse, serviceLen, 2000, 256);
//            String serviceResponseStr = new String(serviceResponse, 0, serviceLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
//            if(serviceResponseStr.contains("OK")) {
//                Log.d("VPOS", "Restarting module with AT+RESTART");
//                String restartCmd = "AT+RESTART\r\n";
//                Log.i("VPOS", "[AT CMD] >>> " + restartCmd.trim());
//                ret = At.Lib_ComSend(restartCmd.getBytes(), restartCmd.length());
//
//                if (ret != 0) {
//                    Log.e("VPOS", "Failed to send AT+RESTART command");
//                    return false;
//                }
//
//                // Wait for restart
//                Log.d("VPOS", "Waiting for module to restart...");
//                Thread.sleep(1000); // Wait 3 seconds for restart
//                byte[] restartrsp = new byte[256];
//                int[] restartrspLen = new int[1];
//                ret = At.Lib_ComRecvAT(restartrsp, restartrspLen, 2000, 256);
//                String restartrspStr = new String(restartrsp, 0, restartrspLen[0]);
//                Log.i("VPOS", "[AT RSP] <<< " + restartrspStr.replace("\r\n", "\\r\\n"));
//                Log.d("VPOS", "=== BLE Service Configuration Completed ===");
//
//                //+++
//                String entryCmd = "+++";
//                Log.i("VPOS", "[AT CMD] >>> " + entryCmd.trim());
//                ret = At.Lib_ComSend(entryCmd.getBytes(), entryCmd.length());
//
//                if (ret != 0) {
//                    Log.e("VPOS", "Failed to send +++ command");
//                    return false;
//                }
//                byte[] rspEntry = new byte[256];
//                int[] rspEntryLen = new int[1];
//                ret = At.Lib_ComRecvAT(rspEntry, rspEntryLen, 1000, 256);
//                String rspEntryStr = new String(rspEntry, 0, rspEntryLen[0]);
//                Log.i("VPOS", "[AT RSP] <<< " + rspEntryStr.replace("\r\n", "\\r\\n"));
//                return false;
//            }
//
//
//            Log.d("VPOS", "=== BLE Service Configuration Completed ===");
//            return true;
//
//        } catch (Exception e) {
//            Log.e("VPOS", "Service configuration error: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//    }
    //ret -1 send error,0 success,-2500 timeout.-2 not contain resPresent
    // private  int atCmdSendrcv(String cmd,String Rsp,int timeout,int iRequestLen,String resPresent) {
    //     // Check current service configuration
    //     Log.d("VPOS", "Checking current cmd");
    //     Log.i("VPOS", "[AT CMD] >>> " + cmd.trim());
    //     int ret = At.Lib_ComSend(cmd.getBytes(), cmd.length());

    //     if (ret != 0) {
    //         Log.e("VPOS", "Failed to send  command:"+cmd.trim());
    //         return -1;
    //     }

    //     byte[] serviceResponse = new byte[2048];
    //     int[] serviceLen = new int[1];
    //     int totalReceived = 0;
    //     long startTime = System.currentTimeMillis();
    //     long currentTime;

    //     // 使用小超时时间轮询接收数据
    //     while ((currentTime = System.currentTimeMillis()) - startTime < timeout) {
    //         // 计算剩余超时时间
    //         int remainingTimeout = (int) (timeout - (currentTime - startTime));
    //         // 使用50ms作为单次接收超时时间，不超过剩余总超时时间
    //         int singleTimeout = Math.min(50, remainingTimeout);
            
    //         byte[] tempBuffer = new byte[1024];
    //         int[] tempLen = new int[1];
    //         ret = At.Lib_ComRecvAT(tempBuffer, tempLen, singleTimeout, iRequestLen - totalReceived);
    //         Log.e("VPOS", "At.Lib_ComRecvAT ret:"+ret);
    //         if (tempLen[0] > 0) {
    //             // 有数据收到，复制到总缓冲区
    //             System.arraycopy(tempBuffer, 0, serviceResponse, totalReceived, tempLen[0]);
    //             totalReceived += tempLen[0];
                
    //             // 再用10ms尝试接收更多数据
    //             try {
    //                 Thread.sleep(10);
    //             } catch (InterruptedException e) {
    //                 e.printStackTrace();
    //             }
                
    //             // 尝试接收更多数据
    //             int moreTimeout = Math.min(10, remainingTimeout - singleTimeout);
    //             if (moreTimeout > 0) {
    //                 byte[] moreBuffer = new byte[1024];
    //                 int[] moreLen = new int[1];
    //                 ret = At.Lib_ComRecvAT(moreBuffer, moreLen, moreTimeout, iRequestLen - totalReceived);
    //                 if (moreLen[0] > 0) {
    //                     // 还有更多数据，复制到总缓冲区
    //                     System.arraycopy(moreBuffer, 0, serviceResponse, totalReceived, moreLen[0]);
    //                     totalReceived += moreLen[0];
    //                 } else {
    //                     // 没有更多数据，结束接收
    //                     break;
    //                 }
    //             }
    //         }
    //         // 如果还没有收到任何数据，继续尝试
    //     }

    //     serviceLen[0] = totalReceived;
    //     String serviceResponseStr = new String(serviceResponse, 0, totalReceived);
    //     if(Rsp!=null)
    //         Rsp=serviceResponseStr;
    //     Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
    //     if(totalReceived!=0) {
    //         if(resPresent.isEmpty())
    //             return 0;
    //         if(serviceResponseStr.toLowerCase().contains(resPresent.toLowerCase()))
    //             return 0;
    //         else
    //             return -2;
    //     }
    //     else
    //         return -2500;
    // }
    //slave mode for update fw using AT+ROLE=0
    private boolean configureBleServices_role0() {
        Log.d("VPOS", "=== BLE Service Configuration ===");
         BleConnection bleConnection = new BleConnection(); 
        try {
            //check at+role=0?
            int iret= bleConnection.atCmdSendrcv("AT+ROLE?\r\n","",1000,256,"AT+ROLE=0");

            if(iret==0){
                Log.e("VPOS", "has already role=0 ");
                 return true;
            }
               
            
            // Check current role configuration
            Log.d("VPOS", "need set at+role=0");

            iret= bleConnection.atCmdSendrcv("AT+ROLE=0\r\n","",4000,256,"OK");
            if(iret!=0){
                Log.e("VPOS", "set role=0 fail");
                return true;
            }
            Log.e("VPOS", "set role=0 sucess");

            Log.d("VPOS", "need restart");

            iret= bleConnection.atCmdSendrcv("AT+RESTART\r\n","",4000,256,"OK");
            if(iret==0){
                Log.e("VPOS", "set RESTART success,can update fw,not enty +++ mode so if out this function its better restart application");
                return true;
            }
//
////            String serviceCmd = "AT+role=0\r\n";
//            String roleCmd = "AT+ROLE=0\r\n";
//            Log.i("VPOS", "[AT CMD] >>> " + roleCmd.trim());
//            int ret = At.Lib_ComSend(roleCmd.getBytes(), roleCmd.length());
//
//            if (ret != 0) {
//                Log.e("VPOS", "Failed to send AT+role=0 command");
//                return false;
//            }
//
//
//
//            byte[] serviceResponse = new byte[256];
//            int[] serviceLen = new int[1];
//            ret = At.Lib_ComRecvAT(serviceResponse, serviceLen, 2000, 256);
//            String serviceResponseStr = new String(serviceResponse, 0, serviceLen[0]);
//            Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
//            if(serviceResponseStr.contains("OK")) {
//                Log.d("VPOS", "Restarting module with AT+RESTART");
//                String restartCmd = "AT+RESTART\r\n";
//                Log.i("VPOS", "[AT CMD] >>> " + restartCmd.trim());
//                ret = At.Lib_ComSend(restartCmd.getBytes(), restartCmd.length());
//
//                if (ret != 0) {
//                    Log.e("VPOS", "Failed to send AT+RESTART command");
//                    return false;
//                }
//
//                // Wait for restart
//                Log.d("VPOS", "Waiting for module to restart...");
//                Thread.sleep(1000); // Wait 3 seconds for restart
//                byte[] restartrsp = new byte[256];
//                int[] restartrspLen = new int[1];
//                ret = At.Lib_ComRecvAT(serviceResponse, serviceLen, 2000, 256);
//                String restartrspStr = new String(serviceResponse, 0, serviceLen[0]);
//                Log.i("VPOS", "[AT RSP] <<< " + serviceResponseStr.replace("\r\n", "\\r\\n"));
//                Log.d("VPOS", "=== BLE Service Configuration Completed ===");
//                return false;
//            }

            Log.d("VPOS", "=== BLE Service Configuration Completed ===");
            return true;

        } catch (Exception e) {
            Log.e("VPOS", "Service configuration error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Navigate to BleConnectActivity with device information
     */
    private void navigateToBleConnect(Device device) {
        // Stop scanning before navigating
        if (startScan) {
            startScan = false;
            At.Lib_AtStopScan();
        }

        Intent intent = new Intent(this, BleConnectActivity.class);
        intent.putExtra(BleConnectActivity.EXTRA_DEVICE_MAC, device.getMacAddress());
        intent.putExtra(BleConnectActivity.EXTRA_DEVICE_NAME, device.getDeviceName());
        intent.putExtra(BleConnectActivity.EXTRA_SERVICE_UUID, device.getServiceUuid());
        startActivity(intent);
    }

    // Legacy dialog method - kept for reference, no longer used
    private void showBleConnectDialog(Device device) {
        // Stop scanning before connecting
        if (startScan) {
            startScan = false;
            At.Lib_AtStopScan();
        }

        // Inflate dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ble_connect, null);

        // Get views
        TextView tvDeviceMac = dialogView.findViewById(R.id.tvDeviceMac);
        View viewStatusIndicator = dialogView.findViewById(R.id.viewStatusIndicator);
        TextView tvBottomStatus = dialogView.findViewById(R.id.tvBottomStatus);
        Button btnCardPayment = dialogView.findViewById(R.id.btnCardPayment);
        Button btnAppPayment = dialogView.findViewById(R.id.btnAppPayment);
        Button btnBack = dialogView.findViewById(R.id.btnBack);

        // Set device info - serviceUUID displayed under customer name
        String serviceUuid = device.getServiceUuid();
        if (serviceUuid == null || serviceUuid.isEmpty()) {
            serviceUuid = "VIP ⭐ | " + device.getMacAddress();
        } else {
            serviceUuid = "VIP ⭐ | " + serviceUuid;
        }
        tvDeviceMac.setText(serviceUuid);

        // Create BLE connection instance
        BleConnection bleConnection = new BleConnection();

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Card Payment button click - Navigate to PaymentActivity
        btnCardPayment.setOnClickListener(v -> {
            Intent intent = new Intent(BeaconActivity.this, PaymentActivity.class);
            intent.putExtra("EXTRA_MODE", "OFFLINE");
            intent.putExtra("EXTRA_AMOUNT", 314100); // Final price from UI
            intent.putExtra("EXTRA_PRODUCT_NAME", "스포츠 상품");
            startActivity(intent);
        });

        // App Payment button click - Send BLE data "order_id=1234"
        btnAppPayment.setOnClickListener(v -> {
            if (!bleConnection.isConnected()) {
                tvBottomStatus.setText("BLE 연결 필요");
                tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
                return;
            }

            btnAppPayment.setEnabled(false);

            new Thread(() -> {
                // Send order_id=1234 via BLE
                String sendData = "order_id=1234";
                BleConnection.SendResult result = bleConnection.sendDataCompleteByMservice(sendData, 4000);

                runOnUiThread(() -> {
                    btnAppPayment.setEnabled(true);

                    if (result.isSuccess()) {
                        // Show success message and navigate to PaymentActivity
                        tvBottomStatus.setText("앱 결제 요청 전송됨");
                        tvBottomStatus.setTextColor(Color.parseColor("#4CAF50"));

                        Intent intent = new Intent(BeaconActivity.this, PaymentActivity.class);
                        intent.putExtra("EXTRA_MODE", "APP");
                        intent.putExtra("EXTRA_AMOUNT", 349000); // Original price
                        intent.putExtra("EXTRA_PRODUCT_NAME", "스포츠 상품");
                        startActivity(intent);
                    } else {
                        tvBottomStatus.setText("전송 실패: " + result.getError());
                        tvBottomStatus.setTextColor(Color.parseColor("#F44336"));
                    }
                });
            }).start();
        });

        // Back button click (close dialog)
        btnBack.setOnClickListener(v -> {
            // Disconnect if still connected
            if (bleConnection.isConnected()) {
                new Thread(() -> {
                    bleConnection.disconnect();
                }).start();
            }
            dialog.dismiss();
        });

        dialog.show();

        // Set dialog to full screen
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Auto-connect when dialog shows
        new Thread(() -> {
            runOnUiThread(() -> {
                viewStatusIndicator.setBackgroundResource(R.drawable.circle_red);
                tvBottomStatus.setText("연결 중...");
                tvBottomStatus.setTextColor(Color.parseColor("#FF9800")); // Orange
            });

            // Execute connection
            BleConnection.ConnectionResult result = bleConnection.connectToDeviceByMservice(device.getMacAddress());

            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    viewStatusIndicator.setBackgroundResource(R.drawable.circle_green);
                    tvBottomStatus.setText("연결됨");
                    tvBottomStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    btnAppPayment.setEnabled(true); // Enable app payment button

                } else {
                    viewStatusIndicator.setBackgroundResource(R.drawable.circle_red);
                    tvBottomStatus.setText("연결 실패");
                    tvBottomStatus.setTextColor(Color.parseColor("#F44336")); // Red
                    btnAppPayment.setEnabled(false); // Keep app payment disabled
                }
            });
        }).start();
    }

    /**
     * Load saved settings and update header texts
     */
    private void loadSettingsToHeader() {


         sp = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        String shopName = sp.getString("shop", "6F 스포츠관 나이키");
        String salesperson = sp.getString("salesperson", "한아름 (224456)");

        tvHeaderLogo.setText(shopName);
        tvHeaderStaff.setText(salesperson);
    }

    /**
     * Show settings dialog
     */
    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etShop = dialogView.findViewById(R.id.etShop);
        EditText etSalesperson = dialogView.findViewById(R.id.etSalesperson);
        EditText etBroadcastName = dialogView.findViewById(R.id.etBroadcastName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnBeaconConfig = dialogView.findViewById(R.id.btn_beacon_config);
        Button btnUuidConfig = dialogView.findViewById(R.id.btn_uuid_config);
        Button btnSlave = dialogView.findViewById(R.id.btn_slave);

        // Load current settings
        SharedPreferences sp = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        SharedPreferences scanSp = getSharedPreferences("scanInfo", MODE_PRIVATE);
        etTitle.setText(sp.getString("title", "VPOS"));
        etShop.setText(sp.getString("shop", "6F 스포츠관 나이키"));
        etSalesperson.setText(sp.getString("salesperson", "한아름 (224456)"));
        etBroadcastName.setText(scanSp.getString("broadcastName", ""));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Make dialog background transparent to show rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Advanced Settings Button Listeners
        btnBeaconConfig.setOnClickListener(v -> {
            dialog.dismiss();
            onClick(v);
        });

        btnUuidConfig.setOnClickListener(v -> {
            dialog.dismiss();
            onClick(v);
        });

        btnSlave.setOnClickListener(v -> {
            dialog.dismiss();
            onClick(v);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String shop = etShop.getText().toString().trim();
            String salesperson = etSalesperson.getText().toString().trim();
            String broadcastName = etBroadcastName.getText().toString().trim();

            // Save settings
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("title", title);
            editor.putString("shop", shop);
            editor.putString("salesperson", salesperson);
            editor.apply();

            // Save broadcastName to scanInfo
            SharedPreferences.Editor scanEditor = scanSp.edit();
            scanEditor.putString("broadcastName", broadcastName);
            scanEditor.apply();

            // Update header
            loadSettingsToHeader();

            dialog.dismiss();
        });

        dialog.show();
    }
}
