# AILock

AILock은 충남대학교 HCI 과제용 스마트폰 절제 앱입니다. 단순히 앱을 막는 대신, 사용자가 제한 앱을 켰을 때 레서판다가 이유를 묻고 AI가 현재 사용 이유와 기록을 바탕으로 허용 여부와 허용 시간을 판단합니다.

## 실행 방법

1. Android Studio에서 이 저장소를 엽니다.
2. Gradle sync를 완료합니다.
3. 에뮬레이터 또는 Android 기기를 연결합니다.
4. 다음 명령으로 디버그 APK를 빌드할 수 있습니다.

```powershell
.\gradlew.bat assembleDebug
```

## 백엔드 연결

기본 AI 판단 엔드포인트는 Spring Boot 백엔드의 `POST /testFinal`입니다.

요청 예시는 다음 형식입니다.

```json
{
  "appName": "Instagram",
  "userInput": "친구가 보낸 과제 공지를 확인해야 해",
  "lockReason": "시험기간인데 인스타를 습관적으로 오래 보게 돼서 줄이고 싶다",
  "currentStats": {
    "willPowerScore": 65,
    "todayOpenAppCount": 5,
    "accumUseApp": 42
  }
}
```

에뮬레이터에서 PC의 `localhost:8080` 서버에 접근하려면 앱의 설정 탭에서 Base URL을 `http://10.0.2.2:8080/`으로 둡니다. Android 에뮬레이터에서 `10.0.2.2`는 호스트 PC의 loopback 주소를 가리키기 때문입니다.

실제 기기에서는 같은 네트워크의 PC IP 또는 공개 서버 주소로 Base URL을 바꾸면 됩니다. 예: `http://192.168.0.12:8080/`.

설정 화면에는 Base URL만 넣는 것을 권장하지만, 실수로 `http://10.0.2.2:8080/testFinal`처럼 엔드포인트까지 넣어도 앱이 자동으로 `/testFinal`을 제거한 뒤 Retrofit에서 다시 붙입니다.

로컬 remind 서버는 다음 순서로 실행합니다.

```powershell
ssh -N -L 15432:127.0.0.1:5432 -L 20251:127.0.0.1:11434 -p 20250 root@168.188.128.36
```

위 터널 창은 유지한 상태에서 다른 터미널에서 서버를 실행합니다.

```powershell
cd C:\Users\ccomet0810\Documents\GitHub\remind
.\gradlew.bat bootRun
```

## 필요한 Android 권한

- Usage Access: 앱 사용 기록과 제한 시간 계산
- Display over other apps: 제한 앱 위 레서판다 팝업 표시
- Accessibility Service: 제한 앱 실행 감지와 HOME 이동 유도
- Notification Permission: 제한 시간 전후 알림

접근성 서비스는 사용자가 명시적으로 켜야 하며, AILock은 제한 앱 감지와 자기조절 보조 목적으로만 사용합니다. 사용자 입력 내용이나 화면 내용을 몰래 수집하지 않습니다.

## Mock AI Mode

설정 탭에서 Mock AI mode를 켜면 백엔드가 꺼져 있어도 전체 플로우가 동작합니다.

Mock 판단은 의지력 점수, 오늘 실행 횟수, 누적 사용량, 사용 이유의 구체성을 바탕으로 `OPTIMAL`, `WARNING`, `OVERUSE`, `CRITICAL`과 임시 허용 시간을 반환합니다. 기본값은 실제 백엔드 요청 우선이며, 설정에서 Mock AI mode를 켜면 서버 호출 없이 mock 판단만 사용합니다.

## 레서판다 SVG 교체

현재 캐릭터는 `app/src/main/res/drawable/red_panda.xml` 벡터 리소스를 사용합니다. 새 SVG를 Android Vector Drawable로 변환한 뒤 같은 파일명으로 교체하면 `RedPandaMascot`, 말풍선, 오버레이 화면에 함께 반영됩니다.

## 현재 구현 한계

- Android 일반 앱은 다른 앱을 완전히 강제 종료할 수 없습니다.
- AILock은 알림, 오버레이, 이유 입력, AI 판단, 서약 흐름을 통해 자발적 종료를 유도합니다.
- 접근성 서비스가 허용된 경우 제한 앱 감지와 HOME 이동 유도에 가까운 데모 동작을 구현합니다.
- 기기 제조사, Android 버전, 앱별 보안 정책에 따라 UsageStats 또는 Overlay 동작이 다를 수 있습니다.
- 현재 Room 대신 DataStore에 사용자 정보, 제한 앱 설정, 기록 데이터를 JSON으로 저장합니다.
