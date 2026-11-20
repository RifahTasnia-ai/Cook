---
name: "Turbo Mode"
description: "A serene, ultra-minimalist, high-precision mobile design system built for low cognitive load, deliberate pre-commitment, and distraction-free execution."
---

## Overview

Turbo Mode is a calm, surgical productivity system designed to convert intention into immediate action. The visual language prioritizes spatial clarity, generous breathing room, and one primary focal point per screen. Surfaces step softly through **Midnight Fern** dark botanical tonal layers (`#0A1412` canvas to `#17322E` elevated cards with subtle `1px` `#27524B` hairline edges) rather than relying on heavy shadows or decorative chrome. Typography pairs geometric precision with humanist legibility and strict tabular numerals so timers, scores, and multipliers stay visually anchored.

## Colors

The Midnight Fern dark botanical palette uses a deep obsidian-green canvas (`#0A1412`), elevated `#17322E` surfaces with subtle `#27524B` hairline borders, `#4D7B73` active accents, `#8FB8AE` secondary metadata, and crisp `#DCEBE6` primary text and CTA fills (paired with `#0A1412` ink). Semantic status hues—`#4D7B73` / `#DCEBE6` for `Active Zone` (>= 50 pts) and muted amber-ochre (`#C98A4B` on `#2A2218`) for `Laziness Zone` (< 50 pts)—communicate objective state calmly.

```yaml
colors:
  primary: "#DCEBE6"
  on-primary: "#0A1412"
  primary-container: "#27524B"
  on-primary-container: "#DCEBE6"
  secondary: "#4D7B73"
  on-secondary: "#F8F9F8"
  secondary-container: "#27524B"
  on-secondary-container: "#DCEBE6"
  surface: "#0A1412"
  surface-container-lowest: "#17322E"
  surface-container-low: "#122724"
  surface-container: "#27524B"
  surface-container-high: "#27524B"
  on-surface: "#DCEBE6"
  on-surface-variant: "#8FB8AE"
  outline: "#8FB8AE"
  outline-variant: "#27524B"
  inverse-surface: "#0A1412"
  inverse-on-surface: "#DCEBE6"
  inverse-primary: "#4D7B73"
  success: "#4D7B73"
  on-success: "#F8F9F8"
  success-container: "#27524B"
  on-success-container: "#DCEBE6"
  warning: "#C98A4B"
  on-warning: "#0A1412"
  warning-container: "#2A2218"
  on-warning-container: "#C98A4B"
  error: "#E06C65"
  on-error: "#0A1412"
  error-container: "#3B1816"
  on-error-container: "#F9DADA"
  scrim: "#0A1412"
```

## Typography

**Albert Sans** anchors headlines, timers, and priority badges with crisp geometric structure. **Commissioner** handles body copy, task titles, and reflection prompts for effortless scanning on 390px mobile viewports. **Source Code Pro** is reserved for live formula math and monospace numeric readouts. Every numeric timer, score, and rating scale uses `tabular-nums` (`fontFeature: "tnum"`).

```yaml
typography:
  display-lg:
    fontFamily: "Albert Sans"
    fontSize: 48px
    fontWeight: 600
    lineHeight: 52px
    letterSpacing: -1px
    fontFeature: "tnum"
  display-lg-mobile:
    fontFamily: "Albert Sans"
    fontSize: 32px
    fontWeight: 600
    lineHeight: 36px
    letterSpacing: -0.5px
    fontFeature: "tnum"
  headline-lg:
    fontFamily: "Albert Sans"
    fontSize: 36px
    fontWeight: 600
    lineHeight: 42px
    letterSpacing: -0.5px
  headline-lg-mobile:
    fontFamily: "Albert Sans"
    fontSize: 24px
    fontWeight: 600
    lineHeight: 30px
    letterSpacing: -0.25px
  headline-md:
    fontFamily: "Albert Sans"
    fontSize: 20px
    fontWeight: 600
    lineHeight: 26px
    letterSpacing: -0.2px
  title-lg:
    fontFamily: "Albert Sans"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: -0.1px
  title-md:
    fontFamily: "Commissioner"
    fontSize: 15px
    fontWeight: 600
    lineHeight: 20px
  body-lg:
    fontFamily: "Commissioner"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
  body-md:
    fontFamily: "Commissioner"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
  body-sm:
    fontFamily: "Commissioner"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 18px
  label-lg:
    fontFamily: "Albert Sans"
    fontSize: 15px
    fontWeight: 600
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: "Commissioner"
    fontSize: 12px
    fontWeight: 600
    lineHeight: 16px
    letterSpacing: 0.25px
  label-sm:
    fontFamily: "Commissioner"
    fontSize: 11px
    fontWeight: 600
    lineHeight: 14px
    letterSpacing: 0.3px
  mono-md:
    fontFamily: "Source Code Pro"
    fontSize: 13px
    fontWeight: 500
    lineHeight: 18px
    fontFeature: "tnum"
```

## Layout & Spacing

Mobile-first single-column fluid grid (`390px` viewport) with generous `20px` horizontal page margins and a strict `4px` / `8px` spacing rhythm. Base dashboards leave ample vertical breathing room between the score card, check-in strip, and priority list. Active focus and silence screens strip peripheral chrome and center a single focal point.

```yaml
spacing:
  gutter-xs: 4px
  gutter-sm: 8px
  gutter-md: 12px
  gutter-lg: 16px
  gutter-xl: 24px
  section: 28px
  page-margin: 20px
```

## Elevation & Depth

Hierarchy relies on **tonal surface stepping** (`surface` -> `surface-container-lowest` / `surface-container-low`) and subtle `1px` `outline-variant` hairlines rather than drop shadows. Modal bottom sheets and floating evaluation dialogs sit over a dimmed `scrim` backdrop (`bg-scrim/40`) with crisp surface contrast.

## Shapes

Clean, Apple-inspired soft geometry: `16px` (`lg`) for primary cards and modal containers, `12px` (`md`) for task rows and input cards, `8px` (`DEFAULT`) for segmented controls and rating tiles, and `9999px` (`full`) for status pills, check-in nodes, and primary thumb-zone action buttons.

```yaml
rounded:
  sm: 6px
  DEFAULT: 8px
  md: 12px
  lg: 16px
  xl: 24px
  full: 9999px
```

## Motion & Micro-interaction

Motion is calm, tactile, and purposeful. State transitions on buttons, rating tiles, and toggles respond within `120ms`; modal sheets and overlays enter smoothly with an ease-out deceleration curve (`240ms`). Only the 3-hour incoming call ring uses a slow, deliberate pulse to command attention.

```yaml
motion:
  state-change:
    duration: 120ms
    delay: 0ms
    timingFunction: ease-out
  surface-enter:
    duration: 240ms
    delay: 0ms
    timingFunction: cubic-bezier(0.2, 0, 0, 1)
  surface-exit:
    duration: 160ms
    delay: 0ms
    timingFunction: cubic-bezier(0.3, 0, 1, 1)
```

## Voice & Content Strategy

- **Tone of voice:** Quiet, direct, non-judgmental, and objective. Never preachy, never uses motivational quotes, and never uses Unicode emojis.
- **Capitalisation:** Sentence case for task descriptions and reflection prompts; concise Title Case for screen titles, badges, and primary action buttons.
- **Terminology:** Always use `Active Zone` (>= 50 pts), `Laziness Zone` (< 50 pts), `Turbo Time Block`, `S-1`, `A-1` to `A-3`, `Time 1` through `Time 7`, and `121s Silence`.
- **Numbers:** Tabular-aligned numerals (`tabular-nums`) for all timers (`02:01`, `42:18`), scores (`54 / 50 pts`), and multipliers (`1.5x`).

## Components

Component implementations live as individual HTML sticker sheet files in
`components/`. Each file contains the canonical, styled HTML components with
`<!-- COMPONENT: name -->` comment markers. Read each file before use.

- **Badge** → `@components/badge.html`: `badge-numeric`, `badge-dot`
- **Button** → `@components/button.html`: `button-primary`, `button-icon`, `button-with-icon`
- **Card** → `@components/card.html`: `card-elevated`, `card-action`, `card-interactive`, `card-interactive-2`
- **Modal** → `@components/modal.html`: `modal-standard`, `modal-alert`, `modal-drawer`
- **Typography** → `@components/typography.html`: `typography-display`, `typography-body`, `typography-label`
