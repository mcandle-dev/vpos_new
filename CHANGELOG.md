# VPOS Beacon 앱 개발 로그

## 2026-01-23 - 대기 화면 UI 대폭 개선 (v1.0.13)

### 변경 개요
- 앱 결제 대기 화면 UI 전면 개편
- 결제 코드 및 QR 코드 가시성 대폭 개선
- MaterialCardView 제거로 레이아웃 단순화
- 페이지 타이틀 변경 (혜택 안내 → 결제 안내)
- 애니메이션 및 취소 버튼 제거

---

### 1. 대기 화면 레이아웃 전면 개편

#### 1.1 activity_ble_connect.xml - 박스 제거 및 직접 배치
**파일**: `app/src/main/res/layout/activity_ble_connect.xml`

**변경 전**:
- MaterialCardView 박스 안에 결제 코드와 QR 코드 배치
- 핸드폰 아이콘, 애니메이션 점, 취소 버튼 포함

**변경 후**:
- MaterialCardView 완전 제거
- 결제 코드와 QR 코드를 LinearLayout에 직접 배치
- 핸드폰 아이콘 제거
- 애니메이션 점(...) 제거
- 취소 버튼 제거

**새로운 구조**:
```xml
<LinearLayout orientation="vertical" gravity="center_horizontal">
    <!-- 메시지 -->
    <TextView text="고객 앱으로 주문 정보가\n전송되었습니다" />
    <TextView text="PC 앱 결제 진행하여 주세요" />

    <!-- 결제 코드 -->
    <TextView text="결제 코드" />
    <TextView id="tvPaymentCode" text="1   2   3   4   5   6"
        textSize="28sp" background="#FFFFFF" padding="12dp" />

    <!-- 구분선 -->
    <View width="180dp" height="1dp" />

    <!-- QR 코드 -->
    <TextView text="QR 코드" />
    <ImageView id="ivQrCode" width="180dp" height="180dp"
        src="@drawable/qr_code_png" background="#FFFFFF" />
    <TextView text="QR 코드를 스캔하세요" />
</LinearLayout>
```

---

### 2. 텍스트 및 코드 가시성 개선

#### 2.1 결제 코드 개선
**변경 사항**:
- 텍스트 크기: 32sp → 36sp → **28sp** (최종)
- 텍스트 색상: 파란색(#1976D2) → **검정색(#000000)**
- 배경: 흰색(#FFFFFF) 추가
- Padding: 12dp
- letterSpacing 속성 제거 (잘림 문제 해결)

#### 2.2 메시지 표시 개선
**추가된 메시지**:
- "고객 앱으로 주문 정보가\n전송되었습니다" (17sp)
- **"PC 앱 결제 진행하여 주세요"** (15sp) - 신규 추가

**gravity 변경**:
- `center` → `center_horizontal`
- 메시지가 화면 상단부터 표시되도록 개선

---

### 3. QR 코드 개선

#### 3.1 qr_code_png.xml - Vector Drawable 재작성
**파일**: `app/src/main/res/drawable/qr_code_png.xml`

**변경 사항**:
- Layer-list → **Vector drawable** 전환
- 21×21 그리드 시스템으로 단순화
- 완전 불투명 검정색(#000000) 사용
- 더 큰 블록 크기로 선명도 향상

**패턴 구성**:
- 3개 코너 마커 (7×7 크기)
- 타이밍 패턴
- 중앙 데이터 블록
- 우측/하단 데이터 블록

#### 3.2 ImageView 설정
**변경 사항**:
- 크기: 240dp → 200dp → **180dp** (최종)
- FrameLayout 래퍼 제거 (회색 배경 프레임 제거)
- 흰색 배경(#FFFFFF) 직접 적용
- Padding: 8dp

---

### 4. 간격 최적화

#### 4.1 전체 padding 축소
**LinearLayout padding**:
- 24dp → **16dp** (상단/하단)
- paddingHorizontal: 24dp 유지

#### 4.2 요소 간 여백 축소
| 요소 | 변경 전 | 변경 후 |
|------|---------|---------|
| 메시지1 여백 | 24dp | **4dp** |
| 메시지2 여백 | 24dp | **16dp** |
| 결제 코드 라벨 여백 | 16dp | **8dp** |
| 결제 코드 여백 | 32dp | **16dp** |
| 구분선 여백 | 32dp | **16dp** |
| QR 코드 라벨 여백 | 16dp | **8dp** |
| QR 코드 여백 | 12dp | **8dp** |

---

### 5. 페이지 타이틀 변경

#### 5.1 activity_ble_connect.xml - 헤더 타이틀 수정
**파일**: `app/src/main/res/layout/activity_ble_connect.xml`

**변경**:
```xml
<!-- 변경 전 -->
<TextView text="혜택 안내" />

<!-- 변경 후 -->
<TextView text="결제 안내" />
```

**위치**: 헤더 좌측 (← 이전 버튼 옆)

---

### 6. BleConnectActivity.java 수정

#### 6.1 애니메이션 코드 제거
**파일**: `app/src/main/java/com/example/apidemo/BleConnectActivity.java`

**제거된 필드**:
```java
- private TextView tvAnimatedDots;
- private Handler dotsAnimationHandler;
- private Runnable dotsAnimationRunnable;
```

**제거된 메서드**:
```java
- startDotsAnimation()
- stopDotsAnimation()
```

#### 6.2 취소 버튼 제거
**제거된 필드**:
```java
- private Button btnCancelWaiting;
```

**제거된 코드**:
```java
// findViewById 제거
- btnCancelWaiting = findViewById(R.id.btnCancelWaiting);

// 클릭 리스너 제거
- btnCancelWaiting.setOnClickListener(v -> showBenefitsScreen());

// onDestroy 간소화
- stopDotsAnimation();  // 제거됨
```

#### 6.3 showWaitingScreen() 메서드 간소화
**변경 사항**:
```java
private void showWaitingScreen() {
    currentState = ViewState.WAITING;

    scrollViewContent.setVisibility(View.GONE);
    layoutWaitingScreen.setVisibility(View.VISIBLE);

    // 상태바: "연결됨" (초록색) 유지
    tvBottomStatus.setText("연결됨");
    tvBottomStatus.setTextColor(Color.parseColor("#4CAF50"));
    viewStatusIndicator.setBackgroundResource(R.drawable.circle_green);

    // 애니메이션 시작 코드 제거됨
}
```

---

### 7. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| **activity_ble_connect.xml** | MaterialCardView 제거, 레이아웃 재구성, 메시지 추가, 타이틀 변경 |
| **BleConnectActivity.java** | 애니메이션 코드 제거, 취소 버튼 제거, showWaitingScreen() 간소화 |
| **qr_code_png.xml** | Vector drawable로 재작성, 선명도 개선 |
| **qr_code_demo.xml** | 기존 drawable 대체 (사용 안 함) |

---

### 8. 테스트 체크리스트

#### 화면 표시
- [ ] "고객 앱으로 주문 정보가 전송되었습니다" 메시지 표시
- [ ] "PC 앱 결제 진행하여 주세요" 메시지 표시
- [ ] 결제 코드 6자리 모두 검정색으로 명확히 표시
- [ ] QR 코드 선명하게 표시
- [ ] 핸드폰 아이콘 없음
- [ ] 애니메이션 점(...) 없음
- [ ] 취소 버튼 없음
- [ ] MaterialCardView 박스 없음

#### 레이아웃
- [ ] 모든 요소가 한 화면에 표시 (스크롤 없음)
- [ ] 간격이 적절하게 조정됨
- [ ] 메시지가 화면 상단부터 표시됨

#### 상태 표시
- [ ] 하단 상태바: "연결됨" (초록색)
- [ ] 페이지 타이틀: "결제 안내"

#### 기능
- [ ] 앱 결제 요청 버튼 클릭 시 대기 화면 표시
- [ ] 뒤로가기 버튼으로 혜택 안내 화면 복귀
- [ ] BLE 연결 상태 유지

---

### 9. 주요 개선 효과

#### 가시성 향상
1. **결제 코드**: 검정색 + 흰색 배경으로 명확히 보임
2. **QR 코드**: Vector drawable로 선명도 대폭 향상
3. **메시지**: 모든 텍스트가 잘 보임

#### UI 단순화
1. **박스 제거**: MaterialCardView 제거로 깔끔한 레이아웃
2. **불필요한 요소 제거**: 핸드폰 아이콘, 애니메이션, 취소 버튼
3. **간격 최적화**: 모든 내용이 한 화면에 표시

#### 코드 간소화
1. **애니메이션 코드 제거**: Handler, Runnable 관련 코드 삭제
2. **취소 버튼 제거**: 버튼 및 리스너 코드 삭제
3. **메서드 단순화**: showWaitingScreen(), onDestroy() 간소화

---

## 2026-01-22 - 대기 화면 추가 및 UI/UX 개선 (v1.0.12)

### 변경 개요
- 앱결제 대기 화면 추가 (애니메이션 포함)
- 스캔 상태 기반 상태 표시 개선
- 할인율 및 카드정보 전달 개선
- 주석 처리된 코드 정리
- 아이콘 리소스 업데이트

---

### 1. 앱결제 대기 화면 추가

#### 1.1 BleConnectActivity.java - ViewState 및 대기 화면 구현
**파일**: `app/src/main/java/com/example/apidemo/BleConnectActivity.java`

**추가된 기능**:
```java
// ViewState enum 추가
private enum ViewState {
    BENEFITS,    // 혜택 안내 화면
    WAITING      // 앱결제 대기 화면
}
```

**대기 화면 구성**:
- "결제 진행 중..." 메시지
- 애니메이션 점 표시 (···)
- "취소" 버튼으로 혜택 화면 복귀

**애니메이션 처리**:
```java
private Handler dotsAnimationHandler;
private Runnable dotsAnimationRunnable;
// 점 개수를 1개 → 2개 → 3개 → 1개 순환
```

#### 1.2 activity_ble_connect.xml - 대기 화면 레이아웃 추가
**파일**: `app/src/main/res/layout/activity_ble_connect.xml`

**추가된 뷰**:
```xml
<LinearLayout id="layoutWaitingScreen" visibility="gone">
    <TextView text="결제 진행 중..." />
    <TextView id="tvAnimatedDots" text="···" />
    <Button id="btnCancelWaiting" text="취소" />
</LinearLayout>
```

---

### 2. 스캔 상태 기반 상태 표시 개선

#### 2.1 BeaconActivity.java - onResume 시 상태 업데이트
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**추가된 메서드**:
```java
private void updateStatusBasedOnScanState() {
    if (startScan) {
        updateStatus(Status.SCANNING);  // 스캔 중
    } else {
        updateStatus(Status.WAITING);   // 대기 중
    }
}
```

**개선 효과**:
- 다른 화면에서 돌아왔을 때 정확한 상태 표시
- CONNECTING 상태가 부적절하게 표시되는 문제 해결

---

### 3. 할인율 및 결제 정보 전달 개선

#### 3.1 BleConnectActivity.java - 카드결제 Intent 데이터 확장
**파일**: `app/src/main/java/com/example/apidemo/BleConnectActivity.java`

**변경 사항**:
```java
// VIP 할인율 상수 추가
private static final double VIP_DISCOUNT_PERCENT = 10.0;

// 카드결제 시 Order 객체 및 할인 정보 전달
intent.putExtra(EXTRA_ORDER, order);
intent.putExtra(EXTRA_DISCOUNT_PERCENT, VIP_DISCOUNT_PERCENT);
intent.putExtra(EXTRA_CARD_INFO, "현대백화점 카드");
```

**목적**:
- PaymentActivity에서 주문 정보와 할인율을 일관되게 사용
- 하드코딩된 금액 대신 Order 객체 기반 계산

---

### 4. 코드 정리

#### 4.1 BeaconActivity.java - 주석 처리된 BLE 설정 코드 제거
**파일**: `app/src/main/java/com/example/apidemo/BeaconActivity.java`

**제거된 내용**:
- configureBleServices_role1() 관련 주석 코드 (~130줄)
- AT+UUID_SCAN, AT+MSERVICE, AT+RESTART 관련 실험 코드
- 더 이상 사용하지 않는 서비스 설정 로직

**효과**:
- 코드 가독성 향상
- 파일 크기 감소

---

### 5. UI 리소스 업데이트

#### 5.1 아이콘 리소스 변경
**파일**:
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_smartphone.xml` (신규)

**변경 내용**:
- 런처 아이콘 배경/전경 업데이트
- 스마트폰 아이콘 추가

---

### 6. 기타 개선 사항

#### 6.1 Order.java - 필드 추가
**파일**: `app/src/main/java/com/example/apidemo/model/Order.java`

- Order 모델 확장 (상세 내용은 git diff 참조)

#### 6.2 DeviceAdapter.java - 개선
**파일**: `app/src/main/java/com/example/apidemo/adapter/DeviceAdapter.java`

- 디바이스 목록 표시 로직 개선

---

### 7. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| **BleConnectActivity.java** | ViewState 추가, 대기 화면 구현, 할인율/카드정보 전달 |
| **activity_ble_connect.xml** | 대기 화면 레이아웃 추가 |
| **BeaconActivity.java** | 스캔 상태 기반 상태 업데이트, 주석 코드 제거 |
| **PaymentActivity.java** | Order 객체 기반 금액 처리 개선 |
| **SuccessActivity.java** | UI 개선 |
| **DeviceAdapter.java** | 디바이스 목록 표시 개선 |
| **Order.java** | 모델 확장 |
| **ic_launcher_background.xml** | 아이콘 업데이트 |
| **ic_launcher_foreground.xml** | 아이콘 업데이트 |
| **ic_smartphone.xml** | 신규 추가 |
| **activity_payment.xml** | UI 개선 |
| **activity_success.xml** | UI 개선 |
| **settings.gradle** | 설정 업데이트 |
| **CLAUDE.md** | 문서 업데이트 |
| **.idea/.name** | IDE 설정 업데이트 |

---

### 8. 테스트 체크리스트

#### 대기 화면
- [ ] 앱결제 버튼 클릭 시 대기 화면 표시
- [ ] 점 애니메이션 정상 작동 (1~3개 순환)
- [ ] 취소 버튼으로 혜택 화면 복귀
- [ ] 결제 완료 시 PaymentActivity로 이동

#### 상태 표시
- [ ] BeaconActivity 진입 시: WAITING 표시
- [ ] 스캔 시작 시: SCANNING 표시
- [ ] 다른 화면 갔다가 돌아올 때 상태 정확히 표시

#### 할인 정보 전달
- [ ] 카드결제 시 Order 객체 전달 확인
- [ ] VIP 할인율 10% 적용 확인
- [ ] 카드정보 "현대백화점 카드" 전달 확인

---

(이전 버전 내용은 그대로 유지...)
