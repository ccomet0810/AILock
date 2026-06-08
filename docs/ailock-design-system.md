# AILock Design System

## Tone

AILock은 레서판다 캐릭터가 사용자를 혼내기보다 붙잡아주는 앱이다. 화면은 장난스럽게 꾸미기보다 조용하고 단단하게 구성하고, 강조가 필요한 순간에만 레서판다 주황을 쓴다.

## Color

- Background: 따뜻한 아이보리 `AppBackground`
- Surface: 카드와 입력 영역의 흰색 `AppSurface`
- Muted Surface: 선택 상태, 약한 채움, 팝업 바탕 `AppSurfaceMuted`
- Primary: 레서판다 주황 `PandaOrange`
- Primary Text: 거의 검정에 가까운 갈색 `AppTextStrong`
- Subtle Text: 보조 설명용 갈색 회색 `AppTextSubtle`
- Border: 크림 섞인 회색 `AppBorder`

## Shape

- 카드, 버튼, 입력창, 네비게이션은 기본 8dp radius를 사용한다.
- 하단 즉시개입 패널처럼 화면 가장자리에 붙는 큰 패널만 별도 radius를 허용한다.
- 그림자는 사용하지 않고, 선과 배경색 차이로 레이어를 구분한다.

## Spacing

- 화면 좌우 기본 여백: 20dp
- 섹션 간격: 18dp
- 카드 내부 여백: 16dp
- 리스트 아이템 내부 여백: 14dp

## Components

- Primary button: 레서판다 주황 배경, 흰색 텍스트
- Secondary button: 흰색 배경, 얇은 테두리
- Card: 흰색 배경, 1dp border, shadow 없음
- Bottom navigation: 배경과 분리되는 연한 크림 floating card, 선택 탭은 fill icon과 텍스트 색상으로만 표시
- Progress/graph: 레서판다 주황을 사용하고, 트랙은 연한 크림색을 사용
- Repeated list: 같은 역할의 항목은 카드 여러 개로 나누지 않고, 하나의 리스트 컨테이너 안에서 1dp 구분선으로 나눈다.
