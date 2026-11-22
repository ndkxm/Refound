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
  name: 'Ví Lười',
  slug: 'vi-luoi',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  scheme: 'viluoi',
  userInterfaceStyle: 'automatic',
  ios: {
    icon: './assets/expo.icon',
    bundleIdentifier: 'com.viluoi.app',
  },
  android: {
    package: 'com.viluoi.app',
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
    'expo-secure-store',
    [
      'expo-audio',
      { microphonePermission: 'Ví Lười dùng micro để bạn ghi khoản chi bằng giọng nói.' },
    ],
    [
      'expo-camera',
      {
        cameraPermission: 'Ví Lười dùng camera để chụp hóa đơn và quét mã QR nhóm.',
        microphonePermission: false,
        recordAudioAndroid: false,
      },
    ],
    [
      'expo-image-picker',
      {
        photosPermission: 'Ví Lười đọc ảnh bạn chọn để lấy thông tin chuyển khoản.',
        cameraPermission: 'Ví Lười dùng camera để chụp hóa đơn.',
        // Không đặt microphonePermission: false: plugin sẽ chặn RECORD_AUDIO của cả app, hỏng ghi giọng nói.
      },
    ],
    [
      'expo-location',
      {
        locationWhenInUsePermission: 'Ví Lười gắn vị trí vào khoản chi để vẽ bản đồ chi tiêu.',
        locationAlwaysAndWhenInUsePermission:
          'Ví Lười dùng vị trí khi chạy nền để nhắc bạn lúc đến vùng hay tiêu nhiều tiền.',
        // Geofence (F32) cần vị trí nền trên Android.
        isAndroidBackgroundLocationEnabled: true,
      },
    ],
    'expo-task-manager',
    // Key Google Maps cho Android: biến môi trường lúc build (EAS), không phải biến EXPO_PUBLIC_.
    // Đổi key thì phải build lại dev build.
    ['react-native-maps', { androidGoogleMapsApiKey: process.env.GOOGLE_MAPS_ANDROID_API_KEY }],
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
  extra: {
    eas: {
      // Project trên EAS (expo.dev). Không phải bí mật; push notification cần id này.
      projectId: '775cb191-5db1-427c-99f8-d5eb2d90e50a',
    },
  },
};

export default config;
