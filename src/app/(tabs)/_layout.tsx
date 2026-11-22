import { Tabs } from 'expo-router';
import { StyleSheet, View, type ColorValue } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Icon, type IconName } from '@/components/ui/icon';
import { RecordTabIcon, useOpenCapture } from '@/features/capture/components/record-tab-button';
import { useThemeColors } from '@/theme/use-theme-colors';

// Mọi icon nằm trong cùng một khung 56x32 (cùng khung với ô Ghi); tab đang chọn có nền nhạt phía sau
// và nét đậm hơn, nên các tab luôn thẳng hàng với nhau và với nhãn.
function tabIcon(name: IconName) {
  return function TabIcon({ color, focused }: { color: ColorValue; focused: boolean }) {
    return (
      <View
        className={`h-8 w-14 items-center justify-center rounded-full ${focused ? 'bg-primary/15' : ''}`}
      >
        <Icon name={name} color={color} strokeWidth={focused ? 2.25 : 1.75} />
      </View>
    );
  };
}

export default function TabsLayout() {
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();
  const openCapture = useOpenCapture();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.muted,
        // lineHeight đủ cao để dấu tiếng Việt (ổ, ồ, ở) không bị cắt.
        tabBarLabelStyle: { fontSize: 12, lineHeight: 16, fontWeight: '600', marginTop: 4 },
        tabBarItemStyle: { paddingTop: 8, paddingBottom: 4 },
        tabBarStyle: {
          height: 68 + insets.bottom,
          paddingBottom: insets.bottom,
          backgroundColor: colors.surface,
          borderTopColor: colors.line,
          borderTopWidth: StyleSheet.hairlineWidth,
          elevation: 0,
          shadowOpacity: 0,
        },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Hôm nay', tabBarIcon: tabIcon('home') }} />
      <Tabs.Screen name="transactions" options={{ title: 'Sổ', tabBarIcon: tabIcon('book') }} />
      <Tabs.Screen
        name="record"
        options={{
          title: 'Ghi',
          tabBarAccessibilityLabel: 'Ghi khoản chi',
          tabBarIcon: RecordTabIcon,
          tabBarInactiveTintColor: colors.ink,
        }}
        // Ô Ghi không phải một tab: chặn chuyển tab, mở modal /capture.
        listeners={{
          tabPress: (e) => {
            e.preventDefault();
            openCapture();
          },
        }}
      />
      <Tabs.Screen name="map" options={{ title: 'Bản đồ', tabBarIcon: tabIcon('pin') }} />
      <Tabs.Screen name="groups" options={{ title: 'Nhóm', tabBarIcon: tabIcon('users') }} />
    </Tabs>
  );
}
