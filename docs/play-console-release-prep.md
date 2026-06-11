# AILock Play Console Release Prep

## Recommended Distribution

Use Google Play internal testing first.

- Upload `app/build/outputs/bundle/release/app-release.aab`.
- Add trusted tester Gmail accounts.
- Share the internal testing opt-in link.
- Avoid sharing APK files as the main distribution path because Play Protect can block sideloaded APKs differently on each device.

## Short Description

AI로 앱 사용을 조절하고, 제한 앱 실행 시 잠깐 멈춰 생각하도록 돕는 자기조절 앱

## Full Description Draft

AILock은 사용자가 직접 선택한 앱의 사용을 조절하도록 돕는 자기조절 앱입니다.

사용자는 제한할 앱과 하루 목표 사용 시간을 설정할 수 있습니다. 제한 앱을 실행하면 AILock이 사용 상황을 감지하고, 필요한 경우 개입 화면을 표시해 사용자가 지금 앱을 계속 사용할지 다시 생각하도록 돕습니다.

주요 기능:
- 제한할 앱 선택
- 앱 사용 시간 확인
- 제한 앱 실행 감지
- 사용 전후 자기점검
- 필요한 경우 개입 화면 표시
- 사용 기록 확인

AILock은 사용자가 직접 설정한 제한 앱을 감지하기 위해 접근성 서비스와 사용량 접근 권한을 사용합니다. 접근성 서비스는 현재 실행 중인 앱의 패키지 이름을 확인하고 개입 화면을 표시하는 목적으로만 사용됩니다. 화면 내용, 키보드 입력, 비밀번호, 메시지 내용은 수집하지 않습니다.

## Accessibility API Disclosure

AILock은 제한 앱 실행을 감지하고 자기조절을 돕는 개입 화면을 표시하기 위해 Android 접근성 서비스를 사용합니다.

접근성 서비스 사용 목적:
- 사용자가 직접 선택한 제한 앱이 실행되었는지 확인
- 제한 조건에 해당하면 AILock 개입 화면 표시
- 사용자가 중단을 선택한 경우 홈 화면 이동을 유도

AILock은 접근성 서비스를 통해 화면 내용, 입력 내용, 비밀번호, 메시지, 결제 정보 등을 읽거나 저장하지 않습니다. 접근성 서비스는 앱 차단 및 자기조절 기능 제공에만 사용됩니다.

## Sensitive Permissions

### Usage Access

사용량 접근 권한은 앱별 사용 시간과 제한 조건을 계산하기 위해 사용합니다. 이 권한을 통해 사용자가 설정한 목표 시간과 실제 사용 시간을 비교합니다.

### Display Over Other Apps

다른 앱 위에 표시 권한은 제한 앱 실행 중 개입 화면을 보여주기 위해 사용합니다.

### Notifications

알림 권한은 앱 사용 세션 상태와 필요한 안내를 사용자에게 전달하기 위해 사용합니다.

### Internet

인터넷 권한은 AI 판단 서버와 통신하기 위해 사용합니다. 전송되는 정보는 기기 식별자, 앱 이름, 사용자가 입력한 자기점검 문구, 목표 사용 시간, 당일 사용 시간 등 AILock 기능 제공에 필요한 데이터로 제한됩니다.

## Data Safety Notes

Play Console 데이터 보안 섹션에는 실제 서버 동작에 맞춰 입력해야 합니다. 현재 앱 코드 기준으로 서버 요청에 포함될 수 있는 데이터는 다음과 같습니다.

- Device ID: Android secure ID 기반 식별자
- App name: 제한 대상 앱 이름
- User input: 사용자가 개입 화면에서 입력한 자기점검 문구
- Usage data: 목표 사용 시간, 당일 사용 시간

수집하지 않는다고 설명할 수 있는 항목:
- 화면 내용
- 키보드 입력 전체
- 비밀번호
- 메시지 내용
- 연락처
- 사진/동영상
- 위치 정보

## HTTPS Recommendation

Current backend default:

`http://210.222.240.170:8080/`

For Play distribution, prefer an HTTPS domain such as:

`https://api.example.com/`

HTTPS is not merely cosmetic. It improves user trust, protects request data in transit, and removes the need for `android:usesCleartextTraffic="true"`. Once an HTTPS endpoint is ready, update `AILOCK_BACKEND_BASE_URL` and remove cleartext traffic from the manifest.
