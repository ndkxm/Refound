import '../global.css';

import { DarkTheme, DefaultTheme, SplashScreen, ThemeProvider } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SystemUI from 'expo-system-ui';
import { useEffect, useMemo } from 'react';
import { View } from 'react-native';

import RootNavigator from '@/features/auth/components/root-navigator';
import { useSessionStore } from '@/stores/session-store';
import { themeVars } from '@/theme/colors';
import { useColorSchemeName, useThemeColors } from '@/theme/use-theme-colors';

// Giữ splash đến khi biết phiên đăng nhập, để không nháy qua màn sai.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const isLoading = useSessionStore((s) => s.status === 'loading');
  const scheme = useColorSchemeName();
  const colors = useThemeColors();

  useEffect(() => {
    if (!isLoading) SplashScreen.hide();
  }, [isLoading]);

  // Nền của cửa sổ Android, để lúc chuyển màn hay mở modal không lóe màu trắng.
  useEffect(() => {
    SystemUI.setBackgroundColorAsync(colors.paper);
  }, [colors.paper]);

  const navigationTheme = useMemo(() => {
    const base = scheme === 'dark' ? DarkTheme : DefaultTheme;
    return {
      ...base,
      colors: {
        ...base.colors,
        primary: colors.accent,
        background: colors.paper,
        card: colors.paper,
        text: colors.ink,
        border: colors.line,
      },
    };
  }, [scheme, colors]);

  return (
    <View style={themeVars[scheme]} className="flex-1 bg-paper">
      <ThemeProvider value={navigationTheme}>
        <RootNavigator />
      </ThemeProvider>
      <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />
    </View>
  );
}
