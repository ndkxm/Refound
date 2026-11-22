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
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
  // projectId của EAS sẽ do `npx eas-cli init` ghi vào đây (xem docs/setup.md).
};

export default config;
