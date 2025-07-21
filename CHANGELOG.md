# Changelog

All notable changes to this project will be documented in this file.

## [2.6.0-75] - 2025-01-XX

### 🎉 Expo SDK 53 Compatibility

This release brings full compatibility with Expo SDK 53 and addresses Android build issues.

### ✨ Added

- Full support for Expo SDK 53
- React Native 0.79 compatibility
- React 19 support
- New Architecture (Fabric/TurboModules) support
- Android API 35 (Android 15) support

### 🔧 Updated

- **Android Gradle Plugin**: Updated from 8.6.1 to 8.2.2
- **Kotlin**: Updated from 1.9.25 to 1.9.10 for stable compatibility
- **Gradle Wrapper**: Updated to 8.4 for proven compatibility
- **Google Play Services**: Updated to compatible versions
  - `play-services-base`: 17.6.0 → 18.3.0
  - `play-services-auth`: 19.2.0 → 21.0.0
- **React Native**: Updated peer dependency to support 0.74.0, 0.76.0, and 0.79.0
- **React**: Updated peer dependency to support React 18 and 19
- **Android Car App Library**: Updated to stable 1.7.0 (from 1.7.0-rc01)
- **Kotlin Coroutines**: Updated to 1.7.3
- **Android SDK versions**:
  - `compileSdkVersion`: 34 → 34 (maintained for stability)
  - `targetSdkVersion`: 34 → 34 (maintained for stability)
  - `minSdkVersion`: 21 → 23 (slight increase for compatibility)
- **NDK version**: Updated to 25.1.8937393

### 🐛 Fixed

- Android build failures with Expo SDK 53
- JVM target compatibility issues between Java and Kotlin
- Gradle memory issues with MaxPermSize deprecation
- Dependency conflicts with newer Android versions
- Build tool compatibility issues

### 🔄 Changed

- Minimum Android version slightly increased from API 21 to API 23 (Android 6.0)
- Updated JVM memory settings to use MaxMetaspaceSize instead of deprecated MaxPermSize
- Added explicit kotlinOptions with jvmTarget = "17" for consistency

### 📝 Documentation

- Updated README with comprehensive Expo SDK 53 setup instructions
- Added troubleshooting section for common Android build issues
- Included migration guide from previous versions
- Added feature overview and template documentation

### 🔧 Technical Improvements

- Better Gradle build caching and performance
- Improved Kotlin compilation with latest compiler version
- Enhanced Android Auto integration with updated libraries
- Better memory management during builds

### ⚠️ Breaking Changes

- **Minimum React Native version**: Now requires 0.74.0+ (was 0.60.0+)
- **Minimum React version**: Now requires 18.0.0+ (was 17.0.2+)

### 📦 Migration Guide

If upgrading from a previous version:

1. **Update your project dependencies**:

   ```bash
   npx expo install expo@^53.0.0 --fix
   ```

2. **Clean your build cache**:

   ```bash
   cd android && ./gradlew clean
   ```

3. **Rebuild your development build**:

   ```bash
   npx expo run:android
   ```

4. **Update your app's minimum Android version** if needed in your `app.json`:
   ```json
   {
     "expo": {
       "android": {
         "minSdkVersion": 23
       }
     }
   }
   ```

### 🙏 Credits

Thanks to all contributors and the React Native community for feedback and testing.

---

## [2.6.0-74] - Previous Release

### Features

- Initial CarPlay and Android Auto support
- Template system for navigation and media apps
- Cross-platform compatibility

---

For older versions, please check the git history.
