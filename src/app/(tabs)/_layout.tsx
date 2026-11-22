import { Tabs } from 'expo-router';
import { StyleSheet, type ColorValue } from 'react-native';

import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Icon, type IconName } from '@/components/ui/icon';
import {
  RecordTabButton,
  TAB_BAR_HEIGHT,
  TAB_ITEM_PADDING,
  TAB_LABEL,
} from '@/features/capture/components/record-tab-button';
import { useThemeColors } from '@/theme/use-theme-colors';

function tabIcon(name: Required<Pick<IconName, 'android' | 'ios'>>) {
  return function TabIcon({ color }: { color: ColorValue }) {
    return <Icon name={name} color={color} size={24} />;
  };
}

export default function TabsLayout() {
  const colors = useThemeColors();
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.ink,
        tabBarInactiveTintColor: colors.muted,
        // lineHeight đủ cao để dấu tiếng Việt (ổ, ồ, ở) không bị cắt.
        tabBarLabelStyle: {
          fontSize: TAB_LABEL.fontSize,
          lineHeight: TAB_LABEL.lineHeight,
          fontWeight: '600',
        },
        tabBarItemStyle: { paddingVertical: TAB_ITEM_PADDING },
        tabBarStyle: {
          height: TAB_BAR_HEIGHT + insets.bottom,
          paddingBottom: insets.bottom,
          backgroundColor: colors.paper,
          borderTopColor: colors.line,
          borderTopWidth: StyleSheet.hairlineWidth,
          elevation: 0,
          shadowOpacity: 0,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: 'Hôm nay', tabBarIcon: tabIcon({ android: 'today', ios: 'sun.max' }) }}
      />
      <Tabs.Screen
        name="transactions"
        options={{
          title: 'Sổ',
          tabBarIcon: tabIcon({ android: 'receipt_long', ios: 'list.bullet.rectangle' }),
        }}
      />
      <Tabs.Screen
        name="record"
        options={{
          title: 'Ghi',
          // Ô Ghi không phải một tab: nút riêng mở modal /capture.
          tabBarButton: () => <RecordTabButton />,
        }}
      />
      <Tabs.Screen
        name="map"
        options={{ title: 'Bản đồ', tabBarIcon: tabIcon({ android: 'map', ios: 'map' }) }}
      />
      <Tabs.Screen
        name="groups"
        options={{ title: 'Nhóm', tabBarIcon: tabIcon({ android: 'group', ios: 'person.2' }) }}
      />
    </Tabs>
  );
}
