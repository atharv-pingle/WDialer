# WDialer 📞

An advanced, privacy-first open-source Android Dialer, Call Manager, and SMS application built with **Jetpack Compose** and **Material 3 Expressive Design**.

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)]()
[![Android Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-orange.svg)]()

---

## 🌟 Overview

**WDialer** is designed as a modern, lightweight, and bloat-free replacement for stock phone dialers. It combines telephony controls, an intelligent SMS messenger, custom in-call interfaces, and announcement-free call recording into an aesthetic and fluid experience.

---

## ✨ Key Features

### 📞 Core Telephony & Smart Dialing
* **Predictive T9 Search:** Instantly locate contacts by dialing digits for names, initials, or phone numbers.
* **Dual-SIM Intelligence:** Seamless multi-SIM selection, per-contact preferred SIM memory, and color-coded SIM slot badges.
* **Speed Dial & Dialpad Memory:** One-tap dialing for assigned contacts (keys 1–9) and persistence of entered digits.
* **Call Confirmation Sheet:** Optional verification dialog displaying contact card and SIM indicator before placing a call.
* **USSD & MMI Support:** Full carrier code and supplementary service support.
* **Call & Spam Blocking:** Integrated blacklist manager for nuisance numbers and private callers.

### 🎨 Material 3 Expressive UI & Liquid Glass
* **Dynamic Color Theming:** Automatically adapts to device wallpaper colors (Material You).
* **Liquid Glass Shader Engine:** Custom AGSL runtime shaders providing frosted glass, chromatic aberration, and inverse backdrop effects.
* **Expressive Animations:** Kinetic digit bouncy spring physics and smooth navigation transitions.
* **Custom Call Backgrounds:** Edge-to-edge caller screens with customizable wallpapers, blur filters, and video playback.

### 💬 Built-in SMS Messenger
* **Conversation Threads:** Fast, organized view for all SMS and MMS chats.
* **Scheduled Messaging:** Queue messages to be sent at a designated time.
* **Floating Quick Reply:** Reply to messages instantly from a floating overlay without leaving your active app.

### 🎛️ In-Call Experience & Smart Sensors
* **Floating Call Bubble:** Draggable overlay when navigating outside the call screen during active calls.
* **Proximity & Angle Guard:** Prevents accidental touches and screen timeouts using ear-orientation angle detection.
* **Rain Mode:** Rejects false moisture-induced screen touches.
* **In-Call Notes:** Floating scratchpad to take notes directly during a call.

### 🛡️ Clean & Play Protect Compliant
* **No Telemetry or Ads:** 100% open-source and respects user privacy.
* **Zero Malicious Heuristics:** Stripped of background dropper payloads, key-filtering accessibility abuse, and suspicious permission combinations to ensure smooth installation.

### ⏺️ Advanced Call Recording
* **Unannounced Call Recording:** Powered by elevated ADB/Shizuku tunneling to capture clear internal audio without artificial restrictions or audible warning prompts.

### 🖥️ Desktop Connectivity & Widgets
* **PC Sync:** Bluetooth companion connectivity for syncing recent calls and remote dialing.
* **Home Screen Widgets:** Quick-dial shortcuts, direct contact cards, and recent call log widgets.

---

## 🏗️ Architecture & Tech Stack

| Layer | Implementation |
| :--- | :--- |
| **Language** | Kotlin 2.3 |
| **UI Framework** | Jetpack Compose with Material 3 & Expressive APIs |
| **Dependency Injection** | Koin (Android + Compose) |
| **Navigation** | Compose Destinations (`io.github.raamcosta`) |
| **Shaders & Graphics** | Android AGSL Runtime Shaders (`liquidglass`) |
| **Image Loading** | Coil Compose |
| **Async & Concurrency** | Kotlin Coroutines & StateFlow |

---

## 🛠️ Building & Developing

### Prerequisites
* **Android Studio** (Ladybug or newer recommended)
* **JDK 17** (Temurin or OpenJDK)
* **Android SDK** API 36 / Build Tools

### Build via Command Line
Clone the repository:
```bash
git clone https://github.com/atharv-pingle/WDialer.git
cd WDialer
```

Make the Gradle wrapper executable:
```bash
chmod +x gradlew
```

#### Build Debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release APK:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

---

## 🚀 Automated CI/CD Pipeline

This repository includes a continuous integration and release pipeline configured via **GitHub Actions** (`.github/workflows/build-apk.yml`).

* **Automated Builds:** Triggered on every push to `main` / `master`, pull requests, tags, or manually via `workflow_dispatch`.
* **Automatic GitHub Releases:** Builds the release APK, renames the output artifact (e.g. `WDialer-v13.0.0.apk`), and publishes a new GitHub Release with direct download links.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.
