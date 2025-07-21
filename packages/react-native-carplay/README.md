# React Native CarPlay

CarPlay and Android Auto support for React Native applications.

## Features

- 📱 **CarPlay Support**: Full iOS CarPlay integration
- 🚗 **Android Auto Support**: Complete Android Auto compatibility  
- 🎯 **Expo SDK 53 Compatible**: Updated for the latest Expo SDK
- 🏗️ **New Architecture Ready**: Supports React Native's New Architecture
- 📍 **Navigation Templates**: Map, List, Grid, and more
- 🎵 **Media Support**: Now Playing templates and audio controls
- 📞 **Communication**: Contact and messaging templates

## Installation

```bash
npm install @g4rb4g3/react-native-carplay
# or
yarn add @g4rb4g3/react-native-carplay
```

## Expo SDK 53 Compatibility

This version has been updated for full compatibility with Expo SDK 53, including:

- ✅ **Android API 34**: Updated to target stable Android version
- ✅ **Kotlin 1.9.10**: Stable Kotlin version with proven compatibility
- ✅ **Gradle 8.4**: Updated build tools for better compatibility
- ✅ **React Native 0.76+**: Support for React Native versions compatible with Expo SDK 53
- ✅ **New Architecture**: Full support for Fabric and TurboModules

## Requirements

### iOS
- iOS 14.0+
- Xcode 16+
- CarPlay capability in your app

### Android  
- Android API 23+ (Android 6.0)
- Android Auto support
- Automotive App Library 1.7.0

## Setup

### iOS Setup

1. Add CarPlay capability to your app:
   ```xml
   <!-- ios/YourApp/Info.plist -->
   <key>UIRequiredDeviceCapabilities</key>
   <array>
     <string>carplay</string>
   </array>
   ```

2. Configure CarPlay scenes in your app delegate.

### Android Setup

1. Add Android Auto permissions to your manifest:
   ```xml
   <!-- android/app/src/main/AndroidManifest.xml -->
   <uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES" />
   <uses-permission android:name="androidx.car.app.MAP_TEMPLATES" />
   <uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />
   ```

2. Configure your CarPlay service in the manifest.

## Basic Usage

```typescript
import { CarPlay } from '@g4rb4g3/react-native-carplay';

// Initialize CarPlay
CarPlay.connect();

// Create a simple list template
const listTemplate = {
  id: 'main-list',
  title: 'My App',
  sections: [{
    items: [
      { id: '1', text: 'Item 1', detailText: 'Detail 1' },
      { id: '2', text: 'Item 2', detailText: 'Detail 2' },
    ]
  }],
  onItemSelect: (item) => {
    console.log('Selected:', item);
  }
};

// Present the template
CarPlay.presentTemplate(listTemplate);
```

## Templates

### List Template
Perfect for displaying lists of items like music, contacts, or navigation destinations.

### Map Template  
Ideal for navigation apps with turn-by-turn directions and points of interest.

### Grid Template
Great for displaying visual content like album covers or category icons.

### Now Playing Template
Essential for music and audio apps to show current playback status.

## Troubleshooting

### Android Build Issues

If you encounter build issues after upgrading to Expo SDK 53:

1. **Clean your build cache**:
   ```bash
   cd android && ./gradlew clean
   ```

2. **Update your project's Gradle version** in `android/gradle/wrapper/gradle-wrapper.properties`:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.1-bin.zip
   ```

3. **Ensure correct SDK versions** in your `android/build.gradle`:
   ```gradle
   compileSdkVersion 34
   targetSdkVersion 34
   minSdkVersion 23
   ```

4. **Update Kotlin version** in your project's `android/build.gradle`:
   ```gradle
   ext.kotlinVersion = '1.9.25'
   ```

### Common Issues

- **"Module not found"**: Make sure you've run `pod install` (iOS) or rebuilt your project (Android)
- **CarPlay not connecting**: Ensure your app has the CarPlay capability and proper entitlements
- **Android Auto not working**: Check that all required permissions are added to your manifest

## Migration from Previous Versions

If upgrading from an older version:

1. Update your dependencies to match Expo SDK 53
2. Rebuild your development build 
3. Test all CarPlay/Android Auto functionality
4. Update any deprecated API usage

## Contributing

We welcome contributions! Please read our contributing guidelines and submit pull requests to the main repository.

## License

MIT License - see LICENSE file for details.

## Support

- 📚 [Documentation](https://github.com/g4rb4g3/react-native-carplay/wiki)
- 🐛 [Issue Tracker](https://github.com/g4rb4g3/react-native-carplay/issues)
- 💬 [Discussions](https://github.com/g4rb4g3/react-native-carplay/discussions)

---

Made with ❤️ for the React Native community 