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
     * Little Endian 역순 처리 후 카드번호(앞 16자리), 전화번호(뒤 4자리) 추출
     * 출력: "1234님 (1234 5678 1234 5678)"
     */
    private String parseServiceUuidForMembership(String serviceUuid) {
        if (serviceUuid == null || serviceUuid.isEmpty()) {
            return "정보 없음";
        }

        try {
            // 공백 제거 및 HEX 문자열 정리
            String hexString = serviceUuid.replace(" ", "").trim();

            // 최소 길이 확인 (16바이트 = 32 hex chars)
            if (hexString.length() < 32) {
                return "데이터 부족";
            }

            // Little Endian 역순 처리 - 바이트 단위로 뒤집기
            StringBuilder reversed = new StringBuilder();
            for (int i = hexString.length() - 2; i >= 0; i -= 2) {
                reversed.append(hexString.substring(i, i + 2));
            }

            String reversedHex = reversed.toString();

            // 숫자만 추출
            StringBuilder digitsOnly = new StringBuilder();
            for (char c : reversedHex.toCharArray()) {
                if (Character.isDigit(c)) {
                    digitsOnly.append(c);
                }
            }

            String digits = digitsOnly.toString();

            if (digits.length() < 20) {
                // 충분한 숫자가 없으면 원본 데이터 표시
                return serviceUuid.length() > 20 ? serviceUuid.substring(0, 20) + "..." : serviceUuid;
            }

            // 전화번호: 뒤 4자리
            String phoneNumber = digits.substring(digits.length() - 4);

            // 카드번호: 앞 16자리를 4자리씩 그룹핑
            String cardDigits = digits.substring(0, 16);
            String cardNumber = cardDigits.substring(0, 4) + " " +
                               cardDigits.substring(4, 8) + " " +
                               cardDigits.substring(8, 12) + " " +
                               cardDigits.substring(12, 16);

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

