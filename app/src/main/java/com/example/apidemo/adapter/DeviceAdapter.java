package com.example.apidemo.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mcandle.vpos.R;
import com.example.apidemo.ble.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private static final int removeBleTime = 3*1000;//3s내 재스캔 없으면 목록에서 제거
    private List<Device> deviceList;
    private OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceAdapter() {
        this.deviceList = new ArrayList<>();
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Service UUID에서 멤버십 정보 파싱
     * 예: "FB 34 9B 5F 80 00 34 12 78 56 34 12 78 56 34 12"
     * Little Endian 역순 처리 후:
     *   - 카드번호: 처음 16 hex chars (8바이트) → "1234 5678 1234 5678"
     *   - 전화번호: 다음 4 hex chars (2바이트) → "1234"
     * 출력: "1234님 (1234 5678 1234 5678)"
     */
    /**
     * Service UUID에서 전화번호 추출
     * @param serviceUuid Service UUID 문자열
     * @return 전화번호 (예: "1234") 또는 null
     */
    public static String parsePhoneNumberFromUuid(String serviceUuid) {
        if (serviceUuid == null || serviceUuid.isEmpty()) {
            return null;
        }

        try {
            String hexString = serviceUuid.replace(" ", "").trim();
            if (hexString.length() < 20) {
                return null;
            }

            // Little Endian 역순 처리
            StringBuilder reversed = new StringBuilder();
            for (int i = hexString.length() - 2; i >= 0; i -= 2) {
                reversed.append(hexString.substring(i, i + 2));
            }

            // 전화번호: 16번째부터 4 hex chars (2바이트)
            return reversed.substring(16, 20);
        } catch (Exception e) {
            Log.e("DeviceAdapter", "parsePhoneNumberFromUuid error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Service UUID에서 카드번호 추출
     * @param serviceUuid Service UUID 문자열
     * @return 카드번호 (예: "1234 5678 1234 5678") 또는 null
     */
    public static String parseCardNumberFromUuid(String serviceUuid) {
        if (serviceUuid == null || serviceUuid.isEmpty()) {
            return null;
        }

        try {
            String hexString = serviceUuid.replace(" ", "").trim();
            if (hexString.length() < 20) {
                return null;
            }

            // Little Endian 역순 처리
            StringBuilder reversed = new StringBuilder();
            for (int i = hexString.length() - 2; i >= 0; i -= 2) {
                reversed.append(hexString.substring(i, i + 2));
            }

            // 카드번호: 처음 16 hex chars (8바이트)를 4자리씩 그룹핑
            String cardHex = reversed.substring(0, 16);
            return cardHex.substring(0, 4) + " " +
                   cardHex.substring(4, 8) + " " +
                   cardHex.substring(8, 12) + " " +
                   cardHex.substring(12, 16);
        } catch (Exception e) {
            Log.e("DeviceAdapter", "parseCardNumberFromUuid error: " + e.getMessage());
            return null;
        }
    }

    private String parseServiceUuidForMembership(String serviceUuid) {
        if (serviceUuid == null || serviceUuid.isEmpty()) {
            return "정보 없음";
        }

        try {
            // 공백 제거 및 HEX 문자열 정리
            String hexString = serviceUuid.replace(" ", "").trim();

            // 최소 길이 확인 (10바이트 = 20 hex chars: 8바이트 카드번호 + 2바이트 전화번호)
            if (hexString.length() < 20) {
                return "데이터 부족";
            }

            // Little Endian 역순 처리 - 바이트 단위로 뒤집기
            StringBuilder reversed = new StringBuilder();
            for (int i = hexString.length() - 2; i >= 0; i -= 2) {
                reversed.append(hexString.substring(i, i + 2));
            }

            String reversedHex = reversed.toString();

            // 카드번호: 처음 16 hex chars (8바이트)
            String cardHex = reversedHex.substring(0, 16);
            String cardNumber = cardHex.substring(0, 4) + " " +
                               cardHex.substring(4, 8) + " " +
                               cardHex.substring(8, 12) + " " +
                               cardHex.substring(12, 16);

            // 전화번호: 다음 4 hex chars (2바이트)
            String phoneNumber = reversedHex.substring(16, 20);

            return phoneNumber + "님 (" + cardNumber + ")";

        } catch (Exception e) {
            Log.e("DeviceAdapter", "parseServiceUuidForMembership error: " + e.getMessage());
            return serviceUuid != null && serviceUuid.length() > 20 ?
                   serviceUuid.substring(0, 20) + "..." : (serviceUuid != null ? serviceUuid : "정보 없음");
        }
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        // 멤버십 정보 파싱 및 표시
        String membershipInfo = parseServiceUuidForMembership(device.getServiceUuid());
        holder.membershipInfoTextView.setText(membershipInfo);

        // MAC 주소
        holder.macAddressTextView.setText(device.getMacAddress());

        // RSSI 정보
        holder.rssiValueTextView.setText(device.getRssi() + " dBm");

        // RSSI 아이콘 색상
        Context context = holder.itemView.getContext();
        int grayColor = ContextCompat.getColor(context, R.color.gray);
        int defaultColor = ContextCompat.getColor(context, R.color.default_text_color);

        if (device.getRssi() == -100) {
            holder.rssiValueTextView.setTextColor(grayColor);
            holder.rssiIcon.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN);
        } else {
            holder.rssiValueTextView.setTextColor(defaultColor);
            holder.rssiIcon.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        }

        // 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void setDeviceList(List<Device> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }

    public void addDevice(Device device) {
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMacAddress().equals(device.getMacAddress())) {
                deviceList.get(i).setRssi(device.getRssi());
                deviceList.get(i).setDeviceName(device.getDeviceName());
                deviceList.get(i).setServiceUuid(device.getServiceUuid());
                notifyItemChanged(i);
                return;
            }
        }
        deviceList.add(device);
        notifyItemInserted(deviceList.size() - 1);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearDeviceList() {
        deviceList.clear();
        notifyDataSetChanged();
    }

    public void updateDevice(Device device) {
        int position = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getMacAddress().equals(device.getMacAddress())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            if (device.getDeviceName() == null || device.getDeviceName().isEmpty()) {
                device.setDeviceName(deviceList.get(position).getDeviceName());
            }
            if (device.getServiceUuid() == null || device.getServiceUuid().isEmpty()) {
                device.setServiceUuid(deviceList.get(position).getServiceUuid());
            }
            deviceList.set(position, device);
            notifyItemChanged(position);
        } else {
            deviceList.add(device);
            notifyItemInserted(deviceList.size() - 1);
        }
        removeDisappearDevice();
    }

    public void removeDisappearDevice() {
        long current_timeStamp = System.currentTimeMillis();
        for (int i = deviceList.size() - 1; i >= 0; i--) {
            if (current_timeStamp - deviceList.get(i).getTimestamp() > removeBleTime) {
                deviceList.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void removeDevice(int position) {
        deviceList.remove(position);
        notifyItemRemoved(position);
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView membershipInfoTextView;
        TextView macAddressTextView;
        TextView rssiValueTextView;
        ImageView rssiIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            membershipInfoTextView = itemView.findViewById(R.id.tvMembershipInfo);
            macAddressTextView = itemView.findViewById(R.id.tvMacAddress);
            rssiValueTextView = itemView.findViewById(R.id.tvRssiValue);
            rssiIcon = itemView.findViewById(R.id.ivRssiIcon);
        }
    }
}

