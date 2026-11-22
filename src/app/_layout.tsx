import '../global.css';

import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

// TODO: khung điều hướng (auth / onboarding / tabs) sẽ thay màn tạm này.
export default function RootLayout() {
  return (
    <>
      <Stack screenOptions={{ headerShown: false }} />
      <StatusBar style="auto" />
    </>
  );
}
