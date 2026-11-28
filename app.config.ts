/// <reference types="node" />

import { existsSync } from 'node:fs';

import type { ExpoConfig } from 'expo/config';

// google-services.json (Firebase, dùng cho push FCM) không được commit.
// - Máy cá nhân: đặt file ở gốc repo.
// - EAS Build: biến môi trường dạng file GOOGLE_SERVICES_JSON (xem docs/setup.md).
// Không có file thì bỏ qua, app vẫn chạy được, chỉ chưa nhận được push trên Android.
const googleServicesFile =
  process.env.GOOGLE_SERVICES_JSON ??
  (existsSync('./google-services.json') ? './google-services.json' : undefined);

const config: ExpoConfig = {
  name: 'Refound',
  slug: 'refound',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'refound',
  userInterfaceStyle: 'automatic',
  ios: {
    icon: './assets/expo.icon',
    bundleIdentifier: 'com.refound.app',
  },
  android: {
    package: 'com.refound.app',
    googleServicesFile,
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  web: {
    output: 'server',
    favicon: './assets/images/favicon.png',
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        backgroundColor: '#208AEF',
        image: './assets/images/splash-icon.png',
        imageWidth: 76,
      },
    ],
    [
      'expo-build-properties',
      {
        // firebase-auth 25 (BOM 35, kéo theo @react-native-firebase 27) biên dịch bằng Kotlin 2.4;
        // Kotlin mặc định của SDK 58 là 2.2 không đọc được. 2.3.x đọc được metadata 2.4.
        android: { kotlinVersion: '2.3.21' },
      },
    ],
    '@maplibre/maplibre-react-native',
    'expo-secure-store',
    '@react-native-firebase/app',
    '@react-native-firebase/auth',
    '@react-native-google-signin/google-signin',
    [
      'expo-location',
      {
        locationWhenInUsePermission:
          'Refound dùng vị trí để ghi nơi bạn nhặt được hoặc làm mất đồ.',
      },
    ],
    [
      'expo-camera',
      {
        cameraPermission: 'Refound dùng camera để quét mã QR khi bàn giao đồ.',
        recordAudioAndroid: false,
      },
    ],
    [
      'expo-image-picker',
      {
        photosPermission: 'Refound cần truy cập ảnh để bạn đính kèm ảnh đồ vật.',
        cameraPermission: 'Refound dùng camera để chụp ảnh đồ vật.',
        microphonePermission: false,
      },
    ],
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
  // EAS không tự ghi được vào file .ts nên điền tay; projectId không phải bí mật.
  owner: 'kiennd711',
  extra: {
    eas: {
      projectId: 'dd47f66d-8c76-4cb4-a414-6e9bf8c01879',
    },
  },
};

export default config;
