# AILock Design System

AILock UI는 조용하고 일관된 도구형 앱을 기준으로 설계한다. 캐릭터와 주황 포인트는 친근함을 만들 때만 쓰고, 기본 화면은 반복 사용과 빠른 탐색을 방해하지 않게 구성한다.

## Color

- Background: `AppBackground` (`#FFFBF5`)
- Surface: `AppSurface` (`#FFFFFF`)
- Muted surface: `AppSurfaceMuted` (`#FFF2E3`)
- Pressed surface: `AppSurfacePressed` (`#FFE4C6`)
- Primary: `PandaOrange` (`#F26F12`)
- Primary text: `AppTextStrong`
- Body text: `AppText`
- Secondary text: `AppTextSubtle`
- Border: `AppBorder`
- Strong border: `AppBorderStrong`

## Shape

- Default card radius: `8dp`
- Default control radius: `8dp`
- Graph bar radius: `3dp`
- Default pill radius: `8dp`
- Bottom sheet top radius: `8dp`, bottom corners `0dp`
- Do not introduce large decorative radii unless the surface is a true modal or illustration container.
- Inner rounded elements should use a smaller radius than the outer container. As a rule, `inner radius = outer radius - inset`.

## Stroke

- Default border width: `1dp`
- Dividers use `HorizontalDivider` with `AppBorder`.
- Strong borders are reserved for floating navigation, compact segmented controls, and active selection controls.
- Header divider may fade in during collapse, but its thickness remains `1dp`.

## Spacing

- Screen horizontal padding: `20dp`
- Section gap: `18dp`
- Content vertical padding: `16dp`
- List gap: `14dp`
- Compact gap: `10dp`
- Card content padding: `16dp`
- List row padding: `14dp`
- Icon/text gap: `12dp`
- Button icon gap: `8dp`
- Button content horizontal padding: `18dp`
- Bottom chrome top padding: `8dp`

## Layout

- Expanded collapsing header height: `168dp`
- Collapsed header height: `64dp`
- Bottom navigation height: `64dp`
- Bottom action gradient area: `132dp`
- Scroll content bottom padding: `bottomActionAreaHeight + sectionGap`
- Primary and secondary button height: `52dp`
- Navigation icon size: `24dp`
- App list icon size: `48dp`
- Compact segmented control: `48dp x 32dp` per segment

## Components

- Cards: white surface, `8dp` radius, `1dp AppBorder`, no shadow.
- Primary button: orange fill, white text, `52dp` height, no elevation.
- Secondary button: white fill, `1dp AppBorder`, `52dp` height.
- Bottom navigation: floating card with `AppSurfaceMuted`, `1dp AppBorderStrong`, `8dp` radius.
- Bottom navigation icons: unselected uses outlined icons with `AppTextSubtle`; selected uses filled icons with `AppTextStrong`.
- Segmented button: Material 3 `SingleChoiceSegmentedButtonRow` / `SegmentedButton`; shape and size are overridden only to match AILock tokens.
- Section title: use the shared `SectionTitle` component. Do not add a section title before the first primary item; add extra top rhythm when a title follows previous content.
- Repeated lists: use one card container and internal `1dp AppBorder` dividers instead of separate floating cards.
- Progress and graph bars: muted track with orange fill, `3dp` radius.

## Implementation Rules

- Prefer tokens from `DesignTokens.kt` over literal `dp` values.
- New reusable UI should live under `ui/components`.
- Keep text sizes from `Typography.kt`; avoid viewport-scaled text.
- Material Icons do not expose a shared stroke-width control. Use icon family, size, and color to control visual weight.
- Avoid shadows unless a component needs real elevation feedback.
- Avoid nested cards. Use full-width sections or one repeated-list container.
