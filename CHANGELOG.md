# VPOS Beacon 앱 개발 로그

## 2026-01-14 - UI 리뉴얼 및 패키지 변경

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
| 2026-01-14 | 1.0.7 | UI 리뉴얼, 패키지명 변경, 설정 다이얼로그 추가 |
