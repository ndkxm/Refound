import '../global.css';

import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

// Khung điều hướng (auth / onboarding / tabs) là việc V06.
export default function RootLayout() {
  return (
    <>
      <Stack screenOptions={{ headerShown: false }} />
      <StatusBar style="auto" />
    </>
  );
}
