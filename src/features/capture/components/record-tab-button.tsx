import * as Haptics from 'expo-haptics';
import { router, usePathname } from 'expo-router';
import { useEffect, useRef } from 'react';
import { Pressable, Text, View } from 'react-native';

import { Icon } from '@/components/ui/icon';
import { useThemeColors } from '@/theme/use-theme-colors';

// Kích thước thanh tab, dùng chung với (tabs)/_layout.tsx để nhãn "Ghi" thẳng hàng với nhãn các tab khác.
export const TAB_BAR_HEIGHT = 66;
export const TAB_ITEM_PADDING = 4;
export const TAB_LABEL = { fontSize: 11, lineHeight: 16 };

/** Ô Ghi ở giữa thanh tab: nút tròn nhô lên, bấm thì mở modal Ghi chứ không chuyển tab. */
export function RecordTabButton() {
  const colors = useThemeColors();
  // Chặn bấm liền nhiều lần trước khi modal kịp mở; nhả khi modal đã đóng (đường dẫn rời /capture).
  // Không dùng focus của màn: nút nằm trên thanh tab, không thuộc màn nào.
  const isOpening = useRef(false);
  const pathname = usePathname();

  useEffect(() => {
    if (pathname !== '/capture') isOpening.current = false;
  }, [pathname]);

  const open = () => {
    if (isOpening.current) return;
    isOpening.current = true;
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    router.push('/capture');
  };

  return (
    <Pressable
      onPress={open}
      accessibilityRole="button"
      accessibilityLabel="Ghi khoản chi"
      className="flex-1 items-center justify-end"
      // +4: nhãn tab mặc định của React Navigation có thêm 4px phía dưới (đo trên thanh tab thật).
      style={{ paddingTop: TAB_ITEM_PADDING, paddingBottom: TAB_ITEM_PADDING + 4 }}
    >
      {({ pressed }) => (
        <>
          {/* Nhô lên khỏi mép thanh tab. Tâm nút vẫn nằm trong thanh, vì Android không nhận chạm ngoài khung cha. */}
          <View
            className="absolute -top-5 h-14 w-14 items-center justify-center rounded-full border-4 border-paper bg-ink"
            style={{ transform: [{ scale: pressed ? 0.94 : 1 }] }}
          >
            <Icon name={{ android: 'add', ios: 'plus' }} color={colors.paper} size={28} />
          </View>
          <Text className="font-semibold text-ink" style={TAB_LABEL}>
            Ghi
          </Text>
        </>
      )}
    </Pressable>
  );
}
