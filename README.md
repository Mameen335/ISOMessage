# ISO8583 POS Payment Simulator 💳

> **An educational Android application simulating a Point-of-Sale (POS) payment terminal and ISO8583 host communication.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026+-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.05-blue.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ⚠️ Important Disclaimer

> This is an **educational/demo application only**.
> It does NOT process real payments, use real encryption keys, or connect to any real payment network.
> All transactions, cryptograms, keys, and data are simulated.
> Do NOT use this code in a real payment environment.

---

## 📖 What is ISO8583?

ISO8583 is the international standard for card payment message exchange. Every time you tap your card, swipe it, or pay online, an ISO8583 message travels between:

```
POS Terminal  →  Acquirer Host  →  Card Network (Visa/MC)  →  Issuer Bank
              ←                ←                           ←
```

An ISO8583 message has three parts:
1. **MTI** — Message Type Indicator (e.g., `0200` = Purchase Request, `0210` = Purchase Response)
2. **Bitmap** — 64-bit presence map telling which Data Elements are present
3. **Data Elements** — The actual fields (PAN, amount, terminal ID, response code, etc.)

---

## ✨ Features

### Transactions Simulated
| Transaction | MTI | Processing Code | Description |
|---|---|---|---|
| Purchase | `0200` / `0210` | `000000` | Standard card purchase |
| Refund | `0200` / `0210` | `200000` | Return funds to cardholder |
| Reversal | `0400` / `0410` | `000000` | Undo approved transaction |
| Settlement | `0500` / `0510` | `920000` | End-of-day reconciliation |
| Balance Inquiry | `0200` / `0210` | `900000` | Check available balance |

### ISO8583 Engine
- MTI parser with full description (version, class, function, origin)
- Bitmap visualizer (bit-level grid display)
- Data Element inspector (DE2–DE55 with educational descriptions)
- ISO message builder (fluent API)
- ISO message parser (JSON ↔ domain model)
- Pretty-print field inspector

### EMV/TLV Simulator
- TLV parser for arbitrary tag-length-value data
- EMV tag registry (9F26 ARQC, 9F36 ATC, 95 TVR, 82 AIP, etc.)
- Fake DE55 builder (chip + contactless variants)
- Educational descriptions for every EMV tag

### Security Education
- Fake PIN block construction (ISO 9564 Format 0 walkthrough)
- Fake MAC generation (HMAC concept demonstration)
- Session key / DUKPT key derivation simulation
- Hex utilities (Luhn check, PAN masking, XOR, hex dump)

### UI Screens (12 screens)
1. **Home** — Transaction launcher + recent history
2. **Purchase** — Full DE configuration + simulation controls
3. **Refund** — Credit return form
4. **Reversal** — Undo transaction form
5. **Settlement** — End-of-day reconciliation
6. **Balance Inquiry** — Card balance check
7. **ISO Message Viewer** — Request/Response/Bitmap/Hex tabs
8. **Transaction Details** — Full transaction record
9. **Receipt** — POS-style receipt display
10. **Logs** — Real-time ISO message log
11. **Settings** — Network and terminal configuration
12. **Developer Tools** — Mockoon endpoint explorer, TLV inspector, Security simulator

---

## 🏗️ Architecture

```
app/
├── iso8583/           # ISO8583 engine (MTI, BitMap, DataElement, Builder, Parser)
├── emv/               # EMV/TLV layer (TlvParser, EmvTag, De55Builder)
├── security/          # Security simulation (HexUtils, FakeSecuritySimulator)
├── domain/
│   ├── model/         # Domain entities (Transaction, PaymentResult, UiState)
│   ├── repository/    # Repository interface (Clean Architecture boundary)
│   └── usecase/       # Business logic (Purchase, Refund, Reversal, Settlement, Balance)
├── data/
│   ├── model/         # Data models (MockoonEnvironment)
│   ├── remote/        # Retrofit API + DTOs
│   └── repository/    # PaymentRepositoryImpl
├── di/                # Hilt modules (NetworkModule, RepositoryModule)
├── util/              # AssetLoader, AppLogger
└── ui/
    ├── navigation/    # AppNavigation (NavHost)
    ├── theme/         # FinTech dark theme (Color, Typography)
    ├── components/    # Reusable Compose components
    └── screens/       # 12 feature screens + ViewModels
```

### Architecture Pattern: Clean Architecture + MVVM

```
UI Layer (Compose + ViewModels)
    ↓ ↑
Domain Layer (UseCases + Repository interface)
    ↓ ↑
Data Layer (Repository impl + Retrofit + DTOs)
```

**Key principles:**
- **Separation of concerns** — each layer has one responsibility
- **Dependency inversion** — domain depends on abstractions, not implementations
- **Testability** — UseCases and domain models have zero Android dependencies
- **Unidirectional data flow** — ViewModel → UiState → Composable

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt 2.56 |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| Async | Coroutines + Flow |
| Serialization | Gson |
| Navigation | Navigation Compose 2.9 |
| Testing | JUnit 4 + MockK |

---

## 🚀 Setup Instructions

### 1. Clone & Open
```bash
git clone https://github.com/YOUR_USERNAME/ISOMessage.git
cd ISOMessage
# Open in Android Studio Meerkat or later
```

### 2. Install Mockoon (optional — for real HTTP communication)
```bash
# Option A: Mockoon Desktop
# Download from https://mockoon.com

# Option B: Mockoon CLI
npm install -g @mockoon/cli
mockoon-cli start --data app/src/main/assets/ISO8583_POS_Payment_Host.json
```

### 3. Configure Network
- **Android Emulator**: Base URL = `http://10.0.2.2:5000/` (default — already set)
- **Physical device**: Set Base URL to your machine's local IP, e.g. `http://192.168.1.100:5000/`
- **No Mockoon**: App still works offline with simulated responses

### 4. Build & Run
```bash
./gradlew assembleDebug
# Or run directly from Android Studio (Shift+F10)
```

### 5. Run Tests
```bash
./gradlew :app:test
```

---

## 💡 Learning Path

### Beginner: Understanding Message Structure
1. Open the app → Home screen
2. Tap **Purchase**
3. Fill in card number and amount
4. Tap **Send Purchase Request**
5. View the **Receipt** → tap **ISO Details**
6. Explore the **REQUEST** and **RESPONSE** tabs — each field is explained

### Intermediate: Bitmap Deep Dive
1. Process any transaction
2. Open ISO Message Viewer → **BITMAP** tab
3. Study which bits are set and which DEs they correspond to
4. Compare the request bitmap vs. response bitmap

### Advanced: EMV & Security
1. Open **Developer Tools** → **EMV TLV** tab
2. Study the TLV structure of DE55
3. Note the ARQC (tag 9F26) — understand why this prevents counterfeit fraud
4. Explore **SECURITY** tab — PIN block construction, MAC generation

---

## 🔬 Mockoon Configuration Reference

The file `app/src/main/assets/ISO8583_POS_Payment_Host.json` defines:

| Endpoint | Method | MTI | Behavior |
|---|---|---|---|
| `/iso` | POST | `0200` → `0210` | Amount ≤ 5000 cents → Approved (RC=00); > 5000 → Declined (RC=51) |
| `/iso/reversal` | POST | `0400` → `0410` | Always approved (RC=00) |
| `/iso/settlement` | POST | `0500` → `0510` | Always balanced (RC=00) |
| `/iso/echo` | POST | `0800` → `0810` | Host connectivity test |
| `/health` | GET | N/A | Server health check |

**Special headers:**
- `X-Simulate-Timeout: true` → Returns RC=68 after 5s delay

---

## 🧪 Test Data

| Scenario | Card Number | Amount |
|---|---|---|
| Approved | `5413330089010012` | ≤ EGP 50.00 |
| Declined (Insufficient Funds) | Any | > EGP 50.00 |
| Timeout | Any | Any + enable timeout toggle |
| Visa Card | `4111111111111111` | ≤ EGP 50.00 |

---

## 📚 ISO8583 Quick Reference

### Key Response Codes (DE39)
| Code | Meaning |
|---|---|
| `00` | Approved |
| `05` | Do Not Honour |
| `51` | Insufficient Funds |
| `54` | Expired Card |
| `55` | Incorrect PIN |
| `68` | Timeout |
| `91` | Issuer Unavailable |

### Processing Codes (DE3)
| Code | Transaction Type |
|---|---|
| `000000` | Purchase |
| `200000` | Refund |
| `900000` | Balance Inquiry |
| `920000` | Settlement |

---

## 🗺️ Roadmap / Suggested Improvements

### Phase 1 — Foundation ✅
- [x] ISO8583 message builder/parser
- [x] All 5 transaction types
- [x] EMV/TLV simulation
- [x] Mockoon backend integration
- [x] 12 UI screens

### Phase 2 — Enhancement
- [ ] Room database for persistent transaction history
- [ ] Transaction search and filtering
- [ ] Export transactions as CSV/PDF
- [ ] Dark/Light theme toggle
- [ ] Locale support (Arabic RTL for Egypt)

### Phase 3 — Advanced Learning
- [ ] Binary TCP socket simulation (real ISO8583 transport)
- [ ] TPDU header simulation
- [ ] Offline transaction queue with auto-retry
- [ ] Batch upload simulation
- [ ] Key Management screen (KEK, ZMK, ZPK simulation)
- [ ] Soft POS mode (NFC tap simulation)

### Phase 4 — Production Patterns
- [ ] Certificate pinning example
- [ ] DataStore for encrypted settings
- [ ] Biometric authentication gate
- [ ] PCI DSS compliance checklist screen

---

## 📁 Project Structure Overview

```
ISOMessage/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── ISO8583_POS_Payment_Host.json   # Mockoon config
│   │   ├── java/com/mameen/isomessage/
│   │   │   ├── iso8583/     # Core ISO8583 engine
│   │   │   ├── emv/         # EMV/TLV simulation
│   │   │   ├── security/    # Security utilities
│   │   │   ├── domain/      # Business logic
│   │   │   ├── data/        # Network + repository
│   │   │   ├── di/          # Hilt DI modules
│   │   │   ├── util/        # Utilities
│   │   │   └── ui/          # Compose screens
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
└── README.md
```

---

## 👨‍💻 About

Built by **Mameen** as a FinTech learning project and Android portfolio showcase.

This project demonstrates:
- Senior-level Android development with Kotlin
- Clean Architecture with strict layer separation
- Payment systems knowledge (ISO8583, EMV, PCI DSS concepts)
- Educational code with detailed inline documentation

---

## 📄 License

```
MIT License — Free to use for educational purposes.
See LICENSE file for details.
```

---

*⚠️ This project is for educational purposes only. Not affiliated with any payment network, card scheme, or financial institution.*
