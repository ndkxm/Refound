import '../global.css';

import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';

import { Banner } from '@/components/ui';
import { queryClient } from '@/lib/query-client';
import { useServerWarmup } from '@/lib/server-warmup';
import { useAuthStore } from '@/stores/auth-store';

// Giữ splash tới khi đọc xong token, tránh nháy màn đăng nhập với người đã đăng nhập.
void SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const status = useAuthStore((s) => s.status);
  const needsProfile = useAuthStore((s) => s.needsProfile);
  const { waking } = useServerWarmup();

  useEffect(() => {
    void useAuthStore.getState().hydrate();
  }, []);

  useEffect(() => {
    if (status !== 'loading') void SplashScreen.hideAsync();
  }, [status]);

  const signedIn = status === 'signedIn';

  return (
    <QueryClientProvider client={queryClient}>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={signedIn && !needsProfile}>
          <Stack.Screen name="index" />
        </Stack.Protected>
        <Stack.Protected guard={signedIn && needsProfile}>
          <Stack.Screen name="(auth)/profile-setup" />
        </Stack.Protected>
        <Stack.Protected guard={!signedIn}>
          <Stack.Screen name="(auth)/login" />
        </Stack.Protected>
      </Stack>
      {waking ? <Banner message="Máy chủ đang khởi động, có thể mất tới 1 phút…" /> : null}
      <StatusBar style="auto" />
    </QueryClientProvider>
  );
}
