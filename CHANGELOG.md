# VPOS Beacon 앱 개발 로그

## 2026-01-17 - Service UUID 파싱 개선 및 주문정보 관리 기능 추가 (v1.0.11)

### 변경 개요
- Service UUID 파싱 로직 버그 수정 (전화번호 추출 오류 해결)
- Order 모델 추가 (주문정보 관리)
- BLE 통신 개선 (order_id 기반 데이터 전송)
- PaymentActivity 상태 표시 한글화 및 BLE Send 추가
- SuccessActivity 설정 아이콘 제거

---

### 1. Service UUID 파싱 로직 수정

#### 1.1 DeviceAdapter.java - parseServiceUuidForMembership 버그 수정
**파일**: `app/src/main/java/com/example/apidemo/adapter/DeviceAdapter.java`

**문제점**:
- Service UUID "FB 34 9B 5F 80 00 34 12 78 56 34 12 78 56 34 12" 파싱 시
- 카드번호는 정상: "1234 5678 1234 5678"
- 전화번호 오류: "5934" (기대값: "1234")

**원인**:
- Little Endian 역순 처리 후 숫자만 추출하는 방식 사용
- HEX 문자 (A-F)를 제외하고 숫자만 추출하는 과정에서 순서가 꼬임

**해결**:
```java
// 변경 전: 숫자만 추출 방식
StringBuilder digitsOnly = new StringBuilder();
for (char c : reversedHex.toCharArray()) {
    if (Character.isDigit(c)) {
        digitsOnly.append(c);
    }
}
String phoneNumber = digits.substring(digits.length() - 4);

// 변경 후: HEX 문자열 직접 사용
String reversedHex = reversed.toString();
String cardHex = reversedHex.substring(0, 16);  // 처음 16 hex chars
String phoneNumber = reversedHex.substring(16, 20);  // 다음 4 hex chars
```

**추가된 Public 메서드**:
- `parsePhoneNumberFromUuid(String)` - 전화번호만 추출
- `parseCardNumberFromUuid(String)` - 카드번호만 추출 (4자리씩 그룹핑)

---

### 2. 전화번호/카드번호 Intent 전달

#### 2.1 BeaconActivity.java - 파싱 데이터 전달
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**변경 사항**:
```java
// navigateToBleConnect() 메서드에 추가
String phoneNumber = DeviceAdapter.parsePhoneNumberFromUuid(device.getServiceUuid());
String cardNumber = DeviceAdapter.parseCardNumberFromUuid(device.getServiceUuid());

intent.putExtra(BleConnectActivity.EXTRA_PHONE_NUMBER, phoneNumber);
intent.putExtra(BleConnectActivity.EXTRA_CARD_NUMBER, cardNumber);
```

#### 2.2 BleConnectActivity.java - Member 객체에 할당
**파일**: `app/src/main/java/com/example/apidemo/BleConnectActivity.java`

**변경 사항**:
```java
// onCreate()에서 Intent 데이터 받기
String phoneNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
String cardNumber = getIntent().getStringExtra(EXTRA_CARD_NUMBER);

// Member 객체에 할당
member.setMemberCode(phoneNumber);  // 전화번호 → memberCode
member.setCardNumber(cardNumber);   // 카드번호
```

---

### 3. Order 모델 추가

#### 3.1 Order.java - 주문정보 클래스 생성
**파일**: `app/src/main/java/com/example/apidemo/model/Order.java` (신규)

**필드**:
```java
private String orderId;      // "260115143"
private String prodName;     // "나이키알파플라이3"
private String prodSize;     // "265"
private String prodColor;    // "블랙"
private int prodPrice;       // 349000
```

**특징**:
- Serializable 구현으로 Intent 전달 가능
- 기본값으로 데모 데이터 제공
- `getDisplayOption()` - "265 / 블랙" 형식 반환

---

### 4. 주문정보 기반 BLE 통신

#### 4.1 BeaconActivity.java - Order 객체 생성 및 전달
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**변경 사항**:
```java
// navigateToBleConnect() 메서드에 추가
Order order = new Order();  // 주문정보 생성
intent.putExtra(BleConnectActivity.EXTRA_ORDER, order);
```

#### 4.2 BleConnectActivity.java - 앱결제 요청 시 order_id 전송
**파일**: `app/src/main/java/com/example/apidemo/BleConnectActivity.java`

**변경 전**:
```java
String sendData = "order_id=1234";  // 하드코딩
```

**변경 후**:
```java
String sendData = "order_id=" + order.getOrderId();  // 실제 주문번호 사용
intent.putExtra("EXTRA_AMOUNT", order.getProdPrice());
intent.putExtra("EXTRA_PRODUCT_NAME", order.getProdName());
```

**Static BleConnection 추가**:
- PaymentActivity와 BLE 연결 공유를 위해 static 필드 추가
- `getSharedBleConnection()` 메서드로 접근

---

### 5. PaymentActivity 개선

#### 5.1 activity_payment.xml - 상태 표시 한글화
**파일**: `app/src/main/res/layout/activity_payment.xml`

**변경**:
```xml
<!-- 하단 상태바 텍스트 -->
<TextView android:text="Connected" />  <!-- 변경 전 -->
<TextView android:text="연결됨" />     <!-- 변경 후 -->
```

#### 5.2 PaymentActivity.java - 카드삽입완료 시 BLE Send
**파일**: `app/src/main/java/com/example/apidemo/PaymentActivity.java`

**추가 기능**:
```java
// 카드삽입완료 버튼 클릭 시
btnCompletePayment.setOnClickListener(v -> {
    sendFinishNotification();  // order_id=finish 전송
});

private void sendFinishNotification() {
    if (bleConnection != null && bleConnection.isConnected()) {
        String sendData = "order_id=finish";
        BleConnection.SendResult result =
            bleConnection.sendDataCompleteByMservice(sendData, 4000);

        if (result.isSuccess()) {
            navigateToSuccess();
        }
    } else {
        navigateToSuccess();  // BLE 없어도 진행
    }
}
```

**특징**:
- BLE 연결이 있으면 "order_id=finish" 전송
- BLE 연결이 없어도 결제 완료 화면으로 이동 (비차단)
- 전송 중 버튼 비활성화 및 "전송 중..." 표시

---

### 6. SuccessActivity UI 개선

#### 6.1 activity_success.xml - 설정 아이콘 제거
**파일**: `app/src/main/res/layout/activity_success.xml`

**변경**:
```xml
<!-- 하단 상태바에서 제거 -->
<ImageView
    android:src="@drawable/ic_settings"
    app:tint="#8899AA" />  <!-- 삭제됨 -->
```

**이유**: 결제 완료 화면에서는 설정 접근이 불필요함

---

### 7. BLE 통신 플로우

```
BeaconActivity
    ↓ (디바이스 선택)
    ↓ Order 생성: orderId="260115143"
    ↓
BleConnectActivity
    ↓ (앱결제 버튼 클릭)
    ↓ BLE Send: "order_id=260115143"
    ↓
PaymentActivity (APP 모드)
    ↓ (자동 완료 후 이동)
    ↓
SuccessActivity

---

BeaconActivity
    ↓ (디바이스 선택)
    ↓
BleConnectActivity
    ↓ (카드결제 버튼 클릭)
    ↓
PaymentActivity (OFFLINE 모드)
    ↓ (카드삽입완료 버튼 클릭)
    ↓ BLE Send: "order_id=finish"
    ↓
SuccessActivity
```

---

### 8. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| **DeviceAdapter.java** | parseServiceUuidForMembership 로직 수정, public 메서드 추가 |
| **BeaconActivity.java** | 전화번호/카드번호/Order Intent 전달 |
| **BleConnectActivity.java** | Member 할당, Order 수신, order_id 전송, static BleConnection |
| **PaymentActivity.java** | BLE Send 추가 (order_id=finish), 상태 한글화 |
| **activity_payment.xml** | "Connected" → "연결됨" |
| **activity_success.xml** | 설정 아이콘 제거 |
| **Order.java** | 신규 생성 (주문정보 모델) |

---

### 9. 테스트 체크리스트

#### Service UUID 파싱
- [ ] UUID "FB 34 9B 5F 80 00 34 12 78 56 34 12 78 56 34 12" 입력 시
- [ ] 전화번호: "1234" 정상 출력
- [ ] 카드번호: "1234 5678 1234 5678" 정상 출력
- [ ] 디바이스 목록: "1234님 (1234 5678 1234 5678)" 표시

#### BLE 통신
- [ ] 앱결제 버튼: "order_id=260115143" 전송 확인 (로그)
- [ ] 카드삽입완료 버튼: "order_id=finish" 전송 확인 (로그)
- [ ] BLE 연결 없을 때도 정상 동작

#### UI
- [ ] PaymentActivity 하단 상태: "연결됨" 한글 표시
- [ ] SuccessActivity 하단 상태바: 설정 아이콘 없음

---

## 2026-01-16 - 설정 화면 리뉴얼 및 고객정보 표시 개선 (v1.0.10)

### 변경 개요
- SettingsActivity 완전 리뉴얼 (별도 Activity로 분리)
- BleConnectActivity 고객정보 표시 형식 변경
- Member 모델에 memberCode, cardNumber 필드 추가
- 비콘 제어 기능 설정 화면으로 이동

---

### 1. SettingsActivity 리뉴얼

#### 1.1 activity_settings.xml - 새 레이아웃
**파일**: `app/src/main/res/layout/activity_settings.xml`

**구조 변경**:
- ConstraintLayout + MaterialCardView → ScrollView + LinearLayout
- 간결한 EditText 스타일 적용
- 섹션별 구분선으로 가독성 향상

**섹션 구성**:
```
├── Header (← 뒤로가기 + "설정" 타이틀)
├── 매장 정보 섹션
│   ├── Title (타이틀)
│   ├── Shop (매장명)
│   └── Salesperson (판매원)
├── BLE 설정 섹션
│   └── Broadcast Name
├── 비콘 설정 섹션
│   ├── Query / Start / Stop 버튼
│   └── 스캔 필터 설정 버튼
├── 고급 설정 섹션
│   └── Config / UUID / Slave 버튼
└── 저장 버튼
```

#### 1.2 SettingsActivity.java - 기능 추가
**파일**: `app/src/main/java/com/example/apidemo/SettingsActivity.java`

**새로운 기능**:
- `queryBeaconParams()` - At.Lib_GetBeaconParams() 호출
- `startBeacon()` - At.Lib_EnableBeacon(true)
- `stopBeacon()` - At.Lib_EnableBeacon(false)
- `showScanFilterDialog()` - 스캔 필터 설정 다이얼로그
- `showBeaconConfigDialog()` - 비콘 파라미터 설정 다이얼로그

**Handler 패턴 적용**:
```java
private Handler handler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_BEACON_QUERY_RESULT:
            case MSG_BEACON_START_RESULT:
            case MSG_BEACON_STOP_RESULT:
            case MSG_BEACON_CONFIG_RESULT:
        }
    }
};
```

---

### 2. BleConnectActivity 고객정보 표시 개선

#### 2.1 고객명 형식 변경
**변경 전**: `김준호 고객님 ✔`
**변경 후**: `김준호 (2200)님`

#### 2.2 카드정보 표시 변경
**변경 전**: `VIP ⭐ | {Service UUID}`
**변경 후**: `VIP | 9410-1234-5678-9012`

#### 2.3 설정 아이콘 제거
- 하단 상태바에서 설정 ImageView 제거
- 혜택안내 화면에서는 설정 접근 불필요

---

### 3. Member 모델 확장

#### 3.1 Member.java - 새 필드 추가
**파일**: `app/src/main/java/com/example/apidemo/model/Member.java`

**추가된 필드**:
```java
private String memberCode;   // "2200"
private String cardNumber;   // "9410-1234-5678-9012"
```

**추가된 메서드**:
```java
// "김준호 (2200)님" 형식
public String getDisplayName() {
    return name + " (" + memberCode + ")님";
}

// "VIP | 9410-1234-5678-9012" 형식
public String getDisplayCardInfo() {
    return grade + " | " + cardNumber;
}
```

---

### 4. 새로 생성된 리소스 파일

| 파일 | 용도 |
|------|------|
| `drawable/edit_text_background.xml` | EditText 배경 (흰색, 둥근 모서리) |
| `drawable/ic_arrow_back.xml` | 뒤로가기 화살표 아이콘 |
| `layout/dialog_beacon_settings.xml` | 비콘 설정 다이얼로그 레이아웃 |

---

### 5. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| SettingsActivity.java | 완전 리뉴얼 - 비콘 제어 기능 추가 |
| activity_settings.xml | 새 레이아웃 적용 |
| BleConnectActivity.java | displayMemberInfo() 수정 |
| activity_ble_connect.xml | 설정 아이콘 제거, placeholder 텍스트 변경 |
| Member.java | memberCode, cardNumber 필드 및 메서드 추가 |
| dialog_beacon_settings.xml | 신규 생성 |
| edit_text_background.xml | 신규 생성 |
| ic_arrow_back.xml | 신규 생성 |

---

### 6. VPOS API 사용 메서드

| 메서드 | 용도 |
|--------|------|
| `At.Lib_GetBeaconParams(beacon)` | 비콘 파라미터 조회 |
| `At.Lib_SetBeaconParams(beacon)` | 비콘 파라미터 설정 |
| `At.Lib_EnableBeacon(true/false)` | 비콘 시작/정지 |

---

## 2026-01-15 - UI 개선 및 Broadcast Name 설정 추가 (v1.0.9)

### 변경 개요
- 설정 다이얼로그에 Broadcast Name 필드 추가
- 디바이스 목록에 MAC 주소와 RSSI 정보 다시 표시
- 결제 완료 화면에 하단 상태 바 추가
- DeviceAdapter 헤더 제거로 코드 단순화

---

### 1. 설정 다이얼로그 개선

#### 1.1 dialog_settings.xml - Broadcast Name 필드 추가
**파일**: `app/src/main/res/layout/dialog_settings.xml`

**추가된 내용**:
- Broadcast Name TextInputLayout 추가
- 기존 판매원 필드 아래에 배치

```xml
<com.google.android.material.textfield.TextInputLayout
    android:hint="Broadcast Name">
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etBroadcastName" />
</com.google.android.material.textfield.TextInputLayout>
```

#### 1.2 BeaconActivity.java - Broadcast Name 저장 로직
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**변경 사항**:
- `showSettingsDialog()`에 etBroadcastName 필드 추가
- scanInfo SharedPreferences에서 broadcastName 로드/저장
- 설정 저장 시 scanInfo에 broadcastName 저장

```java
EditText etBroadcastName = dialogView.findViewById(R.id.etBroadcastName);
SharedPreferences scanSp = getSharedPreferences("scanInfo", MODE_PRIVATE);
etBroadcastName.setText(scanSp.getString("broadcastName", ""));

// 저장 시
SharedPreferences.Editor scanEditor = scanSp.edit();
scanEditor.putString("broadcastName", broadcastName);
scanEditor.apply();
```

---

### 2. 디바이스 목록 UI 복원

#### 2.1 item_device.xml - MAC/RSSI 정보 다시 표시
**파일**: `app/src/main/res/layout/item_device.xml`

**변경 사항**:
- 멤버십 정보 아래에 MAC 주소와 RSSI 정보 다시 추가
- 좌측: MAC 주소 (12sp, 회색)
- 우측: RSSI 아이콘 + 값 (14sp)

```xml
<LinearLayout orientation="horizontal">
    <TextView id="tvMacAddress" />
    <LinearLayout>
        <ImageView id="ivRssiIcon" />
        <TextView id="tvRssiValue" />
    </LinearLayout>
</LinearLayout>
```

#### 2.2 DeviceAdapter.java - 헤더 제거 및 RSSI 표시 복원
**파일**: `app/src/main/java/com/example/apidemo/adapter/DeviceAdapter.java`

**변경 사항**:
- TYPE_HEADER, TYPE_ITEM 상수 제거
- HeaderViewHolder 클래스 제거
- getItemViewType() 메서드 제거
- position 계산에서 헤더 오프셋 제거 (position - 1 → position)
- DeviceViewHolder에 macAddressTextView, rssiValueTextView, rssiIcon 필드 추가
- onBindViewHolder()에 MAC, RSSI 바인딩 코드 추가
- RSSI 값에 따른 아이콘 색상 처리 복원

```java
// RSSI 아이콘 색상 처리
if (device.getRssi() == -100) {
    holder.rssiValueTextView.setTextColor(grayColor);
    holder.rssiIcon.setColorFilter(grayColor, PorterDuff.Mode.SRC_IN);
} else {
    holder.rssiValueTextView.setTextColor(defaultColor);
    holder.rssiIcon.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
}
```

---

### 3. 결제 완료 화면 개선

#### 3.1 activity_success.xml - 하단 상태 바 추가
**파일**: `app/src/main/res/layout/activity_success.xml`

**추가된 내용**:
- 56dp 높이의 하단 상태 바 추가
- 녹색 원형 인디케이터 + "결제 완료" 텍스트
- 설정 아이콘 우측 배치
- 카드뷰 레이아웃 조정 (verticalBias: 0.4)

```xml
<LinearLayout id="layoutBottomBar" height="56dp">
    <View background="@drawable/circle_green" />
    <TextView text="결제 완료" textColor="#4CAF50" />
    <ImageView src="@drawable/ic_settings" />
</LinearLayout>
```

---

### 4. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| BeaconActivity.java | Broadcast Name 설정 로직 추가 |
| DeviceAdapter.java | 헤더 제거, MAC/RSSI 표시 복원 |
| item_device.xml | MAC/RSSI 레이아웃 추가 |
| dialog_settings.xml | Broadcast Name 입력 필드 추가 |
| activity_success.xml | 하단 상태 바 추가 |

---

## 2026-01-14 - UI 통일 및 프로젝트 정리 (v1.0.8)

### 변경 개요
- 모든 화면의 헤더를 56dp로 통일
- 디바이스 목록을 멤버십 정보만 표시하도록 단순화
- Config, UUID, Slave 버튼을 Settings 다이얼로그로 이동
- 사용하지 않는 Activity 및 Layout 파일 삭제 (프로젝트 정리)
- GitHub 신규 저장소(vpos_new) 생성 및 전체 push

---

## 1. UI 단순화 및 통일

### 1.1 item_device.xml - 멤버십 정보만 표시
**파일**: `app/src/main/res/layout/item_device.xml`

**변경 사항**:
- MAC Address TextView 제거
- RSSI TextView 및 ImageView 제거
- 멤버십 정보(tvMembershipInfo)만 표시
- 레이아웃 구조 단순화

**Before**:
```xml
<LinearLayout orientation="vertical">
    <TextView tvMembershipInfo />
    <LinearLayout orientation="horizontal">
        <TextView tvMacAddress />
        <ImageView ivRssiIcon />
        <TextView tvRssiValue />
    </LinearLayout>
</LinearLayout>
```

**After**:
```xml
<LinearLayout orientation="vertical">
    <TextView tvMembershipInfo />
</LinearLayout>
```

### 1.2 DeviceAdapter.java - 관련 코드 제거
**파일**: `app/src/main/java/com/example/apidemo/adapter/DeviceAdapter.java`

**변경 사항**:
- `macAddressTextView`, `rssiValueTextView`, `rssiIcon` 필드 제거
- `onBindViewHolder()`에서 MAC, RSSI 표시 코드 제거
- `DeviceViewHolder`를 `membershipInfoTextView`만 포함하도록 단순화
- 불필요한 import 제거 (ImageView, PorterDuff, ContextCompat)

### 1.3 activity_beacon.xml - 고급 설정 버튼 제거
**파일**: `app/src/main/res/layout/activity_beacon.xml`

**변경 사항**:
- GridLayout에서 Config, UUID, Slave 버튼 제거
- Query, Start, Stop, Scan Config, Scan, Stop Scan 버튼만 유지
- 레이아웃이 더 깔끔하고 간결해짐

### 1.4 dialog_settings.xml - 고급 설정 버튼 추가
**파일**: `app/src/main/res/layout/dialog_settings.xml`

**추가 내용**:
- "고급 설정" 섹션 추가
- Config, UUID, Slave 버튼을 3열 레이아웃으로 배치
- 일반 설정(타이틀, 매장명, 판매원)과 고급 설정 분리

```xml
<TextView text="고급 설정" />
<LinearLayout orientation="horizontal">
    <Button id="btn_beacon_config" text="Config" />
    <Button id="btn_uuid_config" text="UUID" />
    <Button id="btn_slave" text="Slave" />
</LinearLayout>
```

### 1.5 BeaconActivity.java - 설정 다이얼로그 핸들러 추가
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**변경 사항**:

#### initEvent() 수정
```java
// 제거된 버튼
- findViewById(R.id.btn_beacon_config).setOnClickListener(this);
- findViewById(R.id.btn_uuid_config).setOnClickListener(this);
- findViewById(R.id.btn_slave).setOnClickListener(this);
```

#### showSettingsDialog() 확장
```java
Button btnBeaconConfig = dialogView.findViewById(R.id.btn_beacon_config);
Button btnUuidConfig = dialogView.findViewById(R.id.btn_uuid_config);
Button btnSlave = dialogView.findViewById(R.id.btn_slave);

btnBeaconConfig.setOnClickListener(v -> {
    dialog.dismiss();
    onClick(v);
});
// UUID, Slave도 동일하게 처리
```

---

## 2. 화면별 헤더 통일 (56dp)

### 2.1 activity_ble_connect.xml
**파일**: `app/src/main/res/layout/activity_ble_connect.xml`

**변경 전**:
- 헤더 높이: 80dp
- 네비게이션 영역 별도 (layoutNav)
- 이전 버튼과 타이틀이 헤더 아래에 위치

**변경 후**:
- 헤더 높이: **56dp**
- 이전 버튼을 헤더 좌측으로 이동
- "혜택 안내" 타이틀도 헤더로 이동
- 직원명 우측에 유지
- layoutNav 제거, ScrollView가 바로 헤더 아래에 배치

**구조**:
```xml
<RelativeLayout id="layoutHeader" height="56dp">
    <LinearLayout alignParentStart>
        <Button id="btnBack" text="← 이전" />
        <TextView text="혜택 안내" />
    </LinearLayout>
    <TextView id="tvHeaderStaff" alignParentEnd />
</RelativeLayout>
```

### 2.2 activity_payment.xml
**파일**: `app/src/main/res/layout/activity_payment.xml`

**변경 전**:
- 헤더 높이: 80dp
- 네비게이션 영역(layoutNav)에 이전 버튼 + 타이틀 별도 배치

**변경 후**:
- 헤더 높이: **56dp**
- 이전 버튼을 헤더 좌측으로 이동
- "카드 결제" 타이틀도 헤더로 이동
- layoutNav 제거
- 일관된 UI 구조

### 2.3 activity_success.xml
**파일**: `app/src/main/res/layout/activity_success.xml`

**변경 전**:
- 헤더 없음
- 카드 내부에 큰 "결제 완료" 텍스트 (26sp)

**변경 후**:
- 헤더 높이: **56dp** 추가
- "결제 완료" 타이틀이 헤더로 이동 (16sp)
- 직원명 우측에 표시
- 카드 내부의 큰 "결제 완료" 텍스트 제거
- 체크 아이콘만 유지

**구조**:
```xml
<RelativeLayout id="layoutHeader" height="56dp">
    <TextView text="결제 완료" alignParentStart />
    <TextView id="tvHeaderStaff" alignParentEnd />
</RelativeLayout>

<MaterialCardView>
    <FrameLayout> <!-- 체크 아이콘만 --> </FrameLayout>
    <View divider />
    <상품정보, 결제금액 등>
</MaterialCardView>
```

---

## 3. 프로젝트 정리 (Cleansing)

### 3.1 삭제된 Activity (Java 파일 10개)

#### MainActivity에서 연결된 Activity 삭제
1. **MainActivity.java** - 메인 메뉴 화면 (더 이상 사용 안 함)
2. **ComActivity.java** - Serial communication demo
3. **IccActivity.java** - Chip card reader demo
4. **MsrActivity.java** - Magnetic stripe reader demo
5. **PiccActivity.java** - Contactless card reader demo
6. **PrintActivity.java** - Thermal printer demo
7. **ScanActivity.java** - Barcode/QR scanner demo
8. **SysActivity.java** - System utilities

#### ScanActivity에서 사용된 Activity 삭제
9. **barcode/BarcodeScanActivity.java** - Barcode scan
10. **barcode/QRCodeScanActivity.java** - QR code scan

**삭제 후 남은 Activity**:
- `BeaconActivity.java` - 메인 화면 (LAUNCHER)
- `BleConnectActivity.java` - BLE 연결 및 혜택 안내
- `PaymentActivity.java` - 카드 결제
- `SuccessActivity.java` - 결제 완료

### 3.2 삭제된 Layout (9개)

1. **activity_main.xml** - MainActivity 레이아웃
2. **activity_com.xml** - ComActivity 레이아웃
3. **activity_icc.xml** - IccActivity 레이아웃
4. **activity_msr.xml** - MsrActivity 레이아웃
5. **activity_picc.xml** - PiccActivity 레이아웃
6. **activity_print.xml** - PrintActivity 레이아웃
7. **activity_scan.xml** - ScanActivity 레이아웃
8. **activity_sys.xml** - SysActivity 레이아웃
9. **activity_qrcode_scan.xml** - QRCodeScanActivity 레이아웃

**삭제 후 남은 Layout**:
- `activity_beacon.xml`
- `activity_ble_connect.xml`
- `activity_payment.xml`
- `activity_success.xml`
- `dialog_settings.xml`
- `dialog_ble_connect.xml`
- `item_*.xml` (device, header, beacon_info, scan_filter_info)

### 3.3 AndroidManifest.xml 정리
**파일**: `app/src/main/AndroidManifest.xml`

**변경 전**: 13개 Activity 선언
**변경 후**: 4개 Activity만 유지

```xml
<application>
    <activity name="com.example.apidemo.BeaconActivity" exported="true">
        <intent-filter>
            <action name="android.intent.action.MAIN" />
            <category name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity name="com.example.apidemo.BleConnectActivity" />
    <activity name="com.example.apidemo.PaymentActivity" />
    <activity name="com.example.apidemo.SuccessActivity" />
</application>
```

### 3.4 barcode 디렉토리 제거
**디렉토리**: `app/src/main/java/com/example/apidemo/barcode/`

- 두 개의 Activity 삭제 후 빈 디렉토리 제거
- 프로젝트 구조 단순화

---

## 4. GitHub 저장소 생성 및 Push

### 4.1 저장소 정보
- **URL**: https://github.com/mcandle-dev/vpos_new.git
- **Branch**: main
- **초기 커밋**: f258fd2

### 4.2 커밋 정보
```
Initial commit: VPOS Beacon application

- BLE Beacon/Master management with EFR32BG22 module
- Customer payment flow (BleConnect -> Payment -> Success)
- Device scanning with membership info display
- Cleaned up: removed unused activities
- Unified UI with 56dp header bars across all screens
- Advanced settings dialog with Config, UUID, and Slave buttons

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

### 4.3 Push 통계
- **파일 수**: 109개
- **라인 수**: 16,527줄 추가
- **포함 내용**:
  - Java Activity (4개)
  - Layout 파일 (4개 Activity + 다이얼로그 + 아이템)
  - VPOS Library (AAR)
  - Documentation (CLAUDE.md, BeaconActivity_Documentation.md 등)
  - Build 설정 (Gradle, AndroidManifest.xml)
  - GitHub Actions CI 설정

---

## 5. 테스트 체크리스트

### UI 통일 확인
- [ ] BeaconActivity: Config/UUID/Slave 버튼 없음
- [ ] Settings 다이얼로그: 고급 설정 버튼 있음
- [ ] 모든 화면 헤더 높이: 56dp
- [ ] BleConnect: 이전 버튼 + 타이틀이 헤더에 위치
- [ ] Payment: 이전 버튼 + 타이틀이 헤더에 위치
- [ ] Success: 결제 완료 타이틀이 헤더에 위치

### 디바이스 목록 확인
- [ ] 멤버십 정보만 표시 (MAC, RSSI 없음)
- [ ] "1234님 (1234 5678 1234 5678)" 형식
- [ ] 클릭 시 BleConnectActivity 이동

### 빌드 확인
- [ ] Clean build 성공
- [ ] 불필요한 Activity import 없음
- [ ] 삭제된 layout 참조 없음

### GitHub 확인
- [ ] https://github.com/mcandle-dev/vpos_new 접속 가능
- [ ] 파일 목록 정상 표시
- [ ] README 또는 Documentation 확인 가능

---

## 6. 주요 개선 사항 정리

### 사용자 경험 개선
1. **일관된 헤더 디자인**: 모든 화면이 56dp 헤더로 통일되어 앱 전체가 통일감 있음
2. **단순화된 디바이스 목록**: 필수 정보(멤버십)만 표시하여 가독성 향상
3. **설정 구조화**: 일반 설정과 고급 설정 분리로 접근성 개선

### 코드 품질 개선
1. **불필요한 코드 제거**: 8개 Activity + 관련 Layout 삭제로 유지보수성 향상
2. **명확한 책임 분리**: Beacon 기능에만 집중된 구조
3. **파일 구조 단순화**: barcode 디렉토리 제거 등

### 프로젝트 관리 개선
1. **버전 관리 시작**: GitHub 저장소 생성 및 초기 커밋
2. **문서화**: CHANGELOG.md에 모든 변경사항 기록
3. **CI/CD 준비**: GitHub Actions 워크플로우 포함

---

## 7. 향후 계획

### 단기 (v1.1.0)
- [ ] Settings 화면을 별도 Activity로 분리
- [ ] 멤버십 정보 파싱 로직 개선 (에러 처리 강화)
- [ ] 빌드 버전을 1.0.8로 업데이트

### 중기 (v1.2.0)
- [ ] Dark Mode 지원
- [ ] 다국어 지원 (영어, 일본어)
- [ ] 결제 히스토리 기능 추가

### 장기 (v2.0.0)
- [ ] Java 패키지를 com.mcandle.vpos로 마이그레이션
- [ ] Jetpack Compose 마이그레이션 검토
- [ ] 백엔드 API 연동

---

## 2026-01-14 - UI 리뉴얼 및 패키지 변경 (v1.0.7)

### 변경 개요
- VPOS 3893 Beacon 앱을 vpos_scanner 프로젝트 스타일로 UI 전면 개편
- 앱명 및 패키지명 변경 (com.example.apidemo → com.mcandle.vpos)
- 멤버십 정보 표시 기능 추가
- 설정 다이얼로그 구현

---

## 1. 패키지 및 앱 설정 변경

### 1.1 build.gradle
**파일**: `app/build.gradle`

```gradle
// 변경 전
namespace 'com.example.apidemo'
applicationId "com.example.apidemo"
outputFileName = "3893ApiDemo_${variant.buildType.name}_V${defaultConfig.versionName}.apk"

// 변경 후
namespace 'com.mcandle.vpos'
applicationId "com.mcandle.vpos"
outputFileName = "VPOS_${variant.buildType.name}_V${defaultConfig.versionName}.apk"
```

**목적**: 앱 식별자 및 APK 파일명 변경

### 1.2 AndroidManifest.xml
**파일**: `app/src/main/AndroidManifest.xml`

- 모든 Activity 참조를 상대 경로(`.ActivityName`)에서 전체 경로(`com.example.apidemo.ActivityName`)로 변경
- namespace 변경 후 Activity 클래스 탐색 오류 해결

---

## 2. 리소스 파일 추가/수정

### 2.1 strings.xml
**파일**: `app/src/main/res/values/strings.xml`

**추가된 문자열**:
- `app_name`: "ApiDemo" → "VPOS"
- 설정 다이얼로그: `settings`, `title`, `shop`, `salesperson`, `cancel`, `save`
- Beacon Activity: `product_info`, `waiting_customers`

### 2.2 colors.xml
**파일**: `app/src/main/res/values/colors.xml`

**추가된 색상**:
```xml
<color name="md_primary">#1976D2</color>
<color name="gray">#888888</color>
<color name="default_text_color">#333333</color>
```

### 2.3 dimens.xml
**파일**: `app/src/main/res/values/dimens.xml`

**추가된 치수**:
```xml
<dimen name="spacing_small">8dp</dimen>
<dimen name="spacing_medium">12dp</dimen>
<dimen name="spacing_large">16dp</dimen>
<dimen name="touch_target">48dp</dimen>
```

### 2.4 Drawable 파일 생성

#### dialog_background.xml
**파일**: `app/src/main/res/drawable/dialog_background.xml`
- 라운드 코너 배경 (16dp)
- 설정 다이얼로그용

#### round_signal_cellular_alt_24.xml
**파일**: `app/src/main/res/drawable/round_signal_cellular_alt_24.xml`
- RSSI 신호 강도 아이콘
- Material Design 스타일

---

## 3. 레이아웃 변경

### 3.1 item_device.xml (멤버십 스타일)
**파일**: `app/src/main/res/layout/item_device.xml`

**변경 사항**:
- `orientation`: horizontal → **vertical**
- **멤버십 정보 TextView** 추가 (`tvMembershipInfo`)
  - 텍스트: "1234님 (1234 5678 1234 5678)"
  - 크기: 18sp, 굵게, 검정색
- **MAC 주소와 RSSI 가로 배치**
  - MAC: 좌측, 12sp, 회색
  - RSSI: 우측, 아이콘 + 값
- **하단 구분선** 추가 (1dp, #DDDDDD)

### 3.2 activity_beacon.xml
**파일**: `app/src/main/res/layout/activity_beacon.xml`

**주요 변경**:

#### 1) Blue Header Bar 축소
- 높이: 80dp → **56dp** (1줄)
- 내용: 매장명(좌측) + 직원명(우측)

#### 2) Hello Beacon 영역 제거
- `cardMessage` (id: cardMessage) **완전 삭제**
- RecyclerView가 해당 영역까지 확장

#### 3) BeaconMaster 스위치 제거
- 숨겨진 스위치 완전 제거

#### 4) 상품 정보 타이틀 추가
- `tvProductInfoLabel` 추가
- 텍스트: "상품 정보"
- Control Buttons 위에 배치

#### 5) 장치 목록 타이틀 변경
- `tvDeviceListLabel` 텍스트
- "스캔된 장치 목록" → **"결제 대기고객"**

### 3.3 dialog_settings.xml (신규 생성)
**파일**: `app/src/main/res/layout/dialog_settings.xml`

**구성**:
- TextInputLayout 3개 (타이틀, 매장명, 판매원)
- 버튼 2개 (취소, 저장)
- 라운드 배경 적용

---

## 4. Java 코드 변경

### 4.1 DeviceAdapter.java
**파일**: `app/src/main/java/com/example/apidemo/adapter/DeviceAdapter.java`

**주요 변경**:

#### 1) parseServiceUuidForMembership() 메서드 추가
```java
/**
 * Service UUID에서 멤버십 정보 파싱
 * 예: "FB 34 9B 5F 80 00 34 12 78 56 34 12 78 56 34 12"
 * Little Endian 역순 처리 후 카드번호(앞 16자리), 전화번호(뒤 4자리) 추출
 * 출력: "1234님 (1234 5678 1234 5678)"
 */
private String parseServiceUuidForMembership(String serviceUuid)
```

**처리 로직**:
1. HEX 문자열 공백 제거
2. Little Endian 바이트 역순 처리
3. 숫자만 추출
4. 전화번호(뒤 4자리) + 카드번호(앞 16자리, 4자리씩 그룹핑)

#### 2) ViewHolder 업데이트
- 새로운 레이아웃 ID 반영
  - `tvMembershipInfo`
  - `tvMacAddress`
  - `tvRssiValue`
  - `ivRssiIcon`

#### 3) RSSI 아이콘 색상 처리
```java
if (rssi == -100 || rssi < -90) {
    iconColor = gray;
} else {
    iconColor = default_text_color;
}
rssiIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
```

#### 4) R 클래스 import 수정
```java
// 변경 전
import com.example.apidemo.R;

// 변경 후
import com.mcandle.vpos.R;
```

### 4.2 BeaconActivity.java
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**주요 변경**:

#### 1) 상수 추가
```java
private static final String SETTINGS_PREFS = "settingsInfo";
```

#### 2) Header UI 필드 추가
```java
private TextView tvHeaderLogo;
private TextView tvHeaderStaff;
```

#### 3) initView() 수정
- `tv_msg` (Hello Beacon) 관련 코드 제거
- BeaconMaster 스위치 관련 코드 제거
- Header TextView 초기화 추가
- 설정 아이콘 클릭 리스너 추가
- `loadSettingsToHeader()` 호출

#### 4) initData() 수정
- MAC 주소 가져오는 코드 제거
- tv_msg 업데이트 코드 제거

#### 5) SendPromptMsg() 수정
```java
// 변경 전: tv_msg에 표시
tv_msg.setText(strInfo);

// 변경 후: 로그로만 출력
Log.d("BeaconActivity", strInfo);
```

#### 6) promptHandler 수정
- `RECORD_PROMPT_MSG`: tv_msg 업데이트 → 로그 출력
- `STOP_SCAN_DATA_PROMPT_MSG`: tv_msg 업데이트 → 로그 출력
- `SCAN_DATA_PROMPT_MSG`: 스크롤 관련 코드 제거

#### 7) 설정 관련 메서드 추가

**loadSettingsToHeader()**:
```java
private void loadSettingsToHeader() {
    SharedPreferences sp = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
    String shopName = sp.getString("shop", "6F 스포츠관 나이키");
    String salesperson = sp.getString("salesperson", "한아름 (224456)");

    tvHeaderLogo.setText(shopName);
    tvHeaderStaff.setText(salesperson);
}
```

**showSettingsDialog()**:
```java
private void showSettingsDialog() {
    // dialog_settings.xml inflate
    // SharedPreferences에서 현재 설정 불러오기
    // Cancel/Save 버튼 처리
    // 저장 후 loadSettingsToHeader() 호출
}
```

#### 8) R 클래스 import 추가
```java
import com.mcandle.vpos.R;
```

### 4.3 기타 Activity 파일 (R import 추가)

모든 Activity 파일에 R 클래스 import 추가:
- BleConnectActivity.java
- ComActivity.java
- IccActivity.java
- MainActivity.java
- MsrActivity.java
- PaymentActivity.java
- PiccActivity.java
- PrintActivity.java
- ScanActivity.java
- SuccessActivity.java
- SysActivity.java

**변경**:
```java
import com.mcandle.vpos.R;
```

### 4.4 기타 Java 파일 (R import 수정)

- `adapter/DeviceAdapter.java`
- `barcode/BarcodeScanActivity.java`
- `barcode/QRCodeScanActivity.java`
- `ble/DividerItemDecoration.java`

---

## 5. 주요 이슈 및 해결

### Issue #1: R 클래스를 찾을 수 없음
**오류**:
```
error: cannot find symbol
import com.example.apidemo.R;
```

**원인**:
- `build.gradle`에서 `namespace`를 `com.mcandle.vpos`로 변경
- R 클래스 경로가 `com.mcandle.vpos.R`로 생성됨
- Java 파일들은 여전히 `com.example.apidemo.R` import 사용

**해결**:
- 모든 Java 파일의 R import를 `com.mcandle.vpos.R`로 변경

### Issue #2: Activity does not exist
**오류**:
```
Activity class {com.example.apidemo/com.mcandle.vpos.BeaconActivity} does not exist
```

**원인**:
- AndroidManifest.xml에서 상대 경로(`.BeaconActivity`) 사용
- namespace 변경 후 시스템이 `com.mcandle.vpos.BeaconActivity`로 해석
- 실제 Java 파일은 `com.example.apidemo.BeaconActivity`에 위치

**해결**:
- AndroidManifest.xml의 모든 Activity를 전체 경로로 변경
- 예: `.BeaconActivity` → `com.example.apidemo.BeaconActivity`

---

## 6. 테스트 체크리스트

### 빌드 확인
- [ ] `./gradlew assembleDebug` 빌드 성공
- [ ] APK 파일명: `VPOS_debug_V1.0.7.apk` 확인

### 설치 확인
- [ ] 기존 앱과 별도 설치 (패키지명 다름)
- [ ] 앱명 "VPOS"로 표시

### UI 확인

#### Header
- [ ] 높이 56dp (1줄)
- [ ] 매장명 좌측 표시
- [ ] 직원명 우측 표시

#### 메시지 영역
- [ ] Hello Beacon 카드 없음
- [ ] RecyclerView 확장 표시

#### Control Buttons
- [ ] "상품 정보" 타이틀 표시
- [ ] 버튼 배치 정상

#### 장치 목록
- [ ] "결제 대기고객" 타이틀 표시
- [ ] 멤버십 스타일로 표시:
  - 전화번호님 (카드번호) 형식
  - MAC 주소 좌측
  - RSSI 아이콘 + 값 우측
  - 하단 구분선

### 설정 다이얼로그
- [ ] 설정 아이콘 클릭 시 다이얼로그 표시
- [ ] 타이틀, 매장명, 판매원 입력 필드
- [ ] 취소 버튼: 다이얼로그 닫힘
- [ ] 저장 버튼: 설정 저장 후 Header 업데이트
- [ ] 재시작 시 설정 유지

### 기능 테스트
- [ ] Beacon 시작/중지
- [ ] Master 스캔 시작/중지
- [ ] 장치 목록 표시
- [ ] 장치 클릭 시 상세 화면 이동
- [ ] 멤버십 정보 파싱 정상 동작

---

## 7. 참고 사항

### 패키지 구조
```
com.mcandle.vpos (applicationId - 앱 식별용)
└── R class (리소스 클래스)

com.example.apidemo (Java 패키지 - 실제 코드 위치)
├── adapter/
│   └── DeviceAdapter.java
├── barcode/
│   ├── BarcodeScanActivity.java
│   └── QRCodeScanActivity.java
├── ble/
│   ├── BleConnection.java
│   ├── Device.java
│   └── DividerItemDecoration.java
└── BeaconActivity.java (및 기타 Activity들)
```

### 멤버십 정보 파싱 로직
1. Service UUID: "FB 34 9B 5F 80 00 34 12 78 56 34 12 78 56 34 12"
2. 공백 제거: "FB349B5F8000341278563412785634​12"
3. Little Endian 역순: 바이트 단위로 뒤집기
4. 숫자 추출: 숫자만 필터링
5. 전화번호: 뒤 4자리
6. 카드번호: 앞 16자리, 4자리씩 그룹핑
7. 출력: "1234님 (1234 5678 1234 5678)"

### 설정 저장 방식
- SharedPreferences 사용
- 파일명: "settingsInfo"
- 키:
  - "title": 타이틀
  - "shop": 매장명
  - "salesperson": 판매원

---

## 8. 향후 개선 사항

### 제안
1. Java 패키지를 `com.mcandle.vpos`로 전면 마이그레이션
2. 멤버십 정보 파싱 로직 개선 (다양한 포맷 지원)
3. 설정 화면을 별도 Activity로 분리
4. Dark Mode 지원
5. 다국어 지원 (영어)

### 알려진 제한사항
- Java 패키지와 applicationId 불일치
- AndroidManifest에서 전체 경로 사용 필요
- 멤버십 정보 파싱 오류 시 fallback 처리 개선 필요

---

## 작성자
- 날짜: 2026-01-14
- 작업자: Claude Sonnet 4.5

## 변경 이력
| 날짜 | 버전 | 내용 |
|------|------|------|
| 2026-01-14 | 1.0.8 | UI 통일, 프로젝트 정리, GitHub 저장소 생성 |
| 2026-01-14 | 1.0.7 | UI 리뉴얼, 패키지명 변경, 설정 다이얼로그 추가 |
