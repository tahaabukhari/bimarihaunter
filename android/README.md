# Android app (placeholder)

Add your Android Studio project in this `android/` folder. Recommended structure:

- `android/gradlew`, `android/gradle/`, `android/app/`, `android/settings.gradle` etc.

CI will build the project using `./gradlew assembleDebug` and publish APK/AAB artifacts.

Keep sensitive files (keystore, signing configs) out of the repo — store them in CI secrets.
