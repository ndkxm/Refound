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
