import '../global.css';

import { SplashScreen } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';

import RootNavigator from '@/features/auth/components/root-navigator';
import { useSessionStore } from '@/stores/session-store';

// Giữ splash đến khi biết phiên đăng nhập, để không nháy qua màn sai.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const isLoading = useSessionStore((s) => s.status === 'loading');

  useEffect(() => {
    if (!isLoading) SplashScreen.hide();
  }, [isLoading]);

  return (
    <>
      <RootNavigator />
      <StatusBar style="auto" />
    </>
  );
}
