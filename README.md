# Explain My Money - Android App

A native Android app that explains your financial transactions in simple, plain English. All data is processed and stored locally on your device.

## Features

- **SMS Transaction Scanning**: Automatically reads and parses bank transaction SMS messages
- **Bank Statement Import**: Import PDF, CSV, or Excel bank statements
- **Smart Categorization**: Auto-categorizes transactions (Food, Shopping, Investments, etc.)
- **Analytics Dashboard**: View spending patterns with category breakdowns
- **Investment Tracking**: Track SIPs, Mutual Funds, Stocks, PPF, NPS, and more
- **Chat Assistant**: Ask natural language questions about your spending
- **100% Local**: All data stays on your device - nothing is uploaded to any server

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Database**: Room (SQLite)
- **Architecture**: MVVM with Repository pattern
- **File Parsing**: PDFBox-Android, Apache POI, OpenCSV

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (minimum SDK 26)
- Kotlin 1.9.20+

## Setup Instructions

### 1. Download the Project

Download the `android-app` folder to your computer.

### 2. Open in Android Studio

1. Open Android Studio
2. Select "Open" and navigate to the `android-app` folder
3. Wait for Gradle sync to complete (this may take a few minutes)

### 3. Configure the Project

The project should work out of the box. If you encounter issues:

1. Go to **File → Sync Project with Gradle Files**
2. If prompted, install any missing SDK components
3. Make sure you have JDK 17 configured in **File → Project Structure → SDK Location**

### 4. Build and Run

1. Connect an Android device (USB debugging enabled) or start an emulator
2. Click the green "Run" button or press `Shift + F10`
3. Select your device and wait for the app to install

### 5. Build Release APK

To create a signed APK for distribution:

1. Go to **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Create or select a keystore
4. Choose **release** build variant
5. The APK will be in `app/build/outputs/apk/release/`

## Project Structure

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/explainmymoney/
│   │   │   ├── data/
│   │   │   │   ├── database/       # Room database, DAOs
│   │   │   │   ├── parser/         # SMS and statement parsers
│   │   │   │   └── repository/     # Data repository
│   │   │   ├── domain/
│   │   │   │   └── model/          # Data models
│   │   │   ├── ui/
│   │   │   │   ├── components/     # Reusable UI components
│   │   │   │   ├── navigation/     # Navigation setup
│   │   │   │   ├── screens/        # App screens
│   │   │   │   └── theme/          # App theme and styling
│   │   │   ├── utils/              # Utilities (SMS receiver)
│   │   │   ├── ExplainMyMoneyApp.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/                    # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts                # Root build config
├── settings.gradle.kts
└── gradle.properties
```

## Permissions Required

The app requires these permissions:

- **READ_SMS**: To scan transaction SMS messages
- **RECEIVE_SMS**: To detect new transaction messages in real-time
- **READ_EXTERNAL_STORAGE**: To import bank statement files (PDF, CSV, Excel)

All permissions are requested at runtime with clear explanations.

## How It Works

### SMS Parsing
The app scans SMS messages from known bank sender IDs (HDFCBK, SBIINB, ICICIB, etc.) and extracts:
- Transaction amount
- Transaction type (debit/credit)
- Merchant name
- Payment method (UPI, NEFT, IMPS, Card)
- Reference number
- Account balance

### Statement Parsing
Supports bank statements in:
- **PDF**: Extracts text and parses transaction patterns
- **CSV**: Reads standard bank export formats
- **Excel (XLS/XLSX)**: Parses worksheet data

### Categorization
Transactions are automatically categorized based on keywords:
- Food (Zomato, Swiggy, restaurants)
- Shopping (Amazon, Flipkart, Myntra)
- Entertainment (Netflix, Spotify, movies)
- Utilities (electricity, internet, mobile)
- Investments (SIP, mutual funds, stocks)
- EMI (home loan, car loan)

## Privacy

**All your data stays on your device:**
- No data is sent to any server
- No analytics or tracking
- No account required
- Database is stored in app's private storage
- Uninstalling the app removes all data

## Customization

### Adding New Categories
Edit `TransactionCategory` enum in `domain/model/Transaction.kt` and update the categorization logic in `data/parser/SmsParser.kt` and `data/parser/StatementParser.kt`.

### Modifying Theme
Edit colors in `ui/theme/Theme.kt` to customize the app's appearance.

### Adding New Bank Sender IDs
Update the `bankSenders` list in `data/parser/SmsParser.kt` to support additional banks.

## Troubleshooting

### App crashes on startup
- Ensure you have Android SDK 34 installed
- Try "Build → Clean Project" then "Build → Rebuild Project"

### Gradle sync fails
- Check your internet connection
- Try "File → Invalidate Caches and Restart"

### SMS permissions not working
- Make sure the device has SMS capability
- On some devices, you may need to grant permissions manually in Settings

## License

This project is for personal use. All financial data processing happens locally on your device.

---

**Disclaimer**: This app only explains past transactions. It does not provide financial advice, investment recommendations, or suggestions for future actions.
