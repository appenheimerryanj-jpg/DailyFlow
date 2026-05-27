# DailyFlow

DailyFlow is a practical day-to-day Android app built in Kotlin + Jetpack Compose. It includes tasks, habits, quick notes, local persistence, and Google Play Billing for a one-time Premium unlock.

## What is included
- Native Android project
- Jetpack Compose UI
- Local data storage with SharedPreferences + Gson
- Google Play Billing Library 9.0.0
- One-time in-app purchase product ID: `dailyflow_premium_lifetime`
- Free limits:
  - 7 tasks
  - 3 habits
  - 5 notes
- Premium unlock:
  - Unlimited tasks
  - Unlimited habits
  - Unlimited notes

## Required before release
You must create these yourself because they require your private accounts/keys:
1. Google Play Developer account
2. App listing details
3. Privacy policy URL
4. Upload keystore
5. Google Play in-app product with this exact product ID: `dailyflow_premium_lifetime`
6. Test track with licensed testers

## Build locally
Open this folder in Android Studio.

Recommended Android Studio steps:
1. File > Open > select the `DailyFlow` folder
2. Let Gradle sync
3. Run on your Android phone or emulator

## Create upload keystore
Run this from the project root:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dailyflow
```

Then set environment variables before release builds:

```bash
export DAILYFLOW_KEYSTORE=upload-keystore.jks
export DAILYFLOW_KEYSTORE_PASSWORD='your_store_password'
export DAILYFLOW_KEY_ALIAS=dailyflow
export DAILYFLOW_KEY_PASSWORD='your_key_password'
```

Build release bundle:

```bash
./gradlew bundleRelease
```

Output:
`app/build/outputs/bundle/release/app-release.aab`

## Google Play Console setup
1. Create app: DailyFlow
2. Monetize > Products > In-app products
3. Create product ID: `dailyflow_premium_lifetime`
4. Set type: managed product / one-time product
5. Set price, for example $4.99
6. Activate product
7. Add license testers
8. Upload AAB to internal testing
9. Test purchase flow before production

## Important release notes
Google Play requires Google Play Billing for digital goods sold inside Play-distributed apps. This app uses Play Billing for the Premium digital unlock.

Do not upload debug builds to production. Only upload the signed release `.aab`.
