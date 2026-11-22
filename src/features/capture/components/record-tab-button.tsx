import * as Haptics from 'expo-haptics';
import { router, usePathname } from 'expo-router';
import { useCallback, useEffect, useRef } from 'react';
import { View } from 'react-native';

import { Icon } from '@/components/ui/icon';
import { useThemeColors } from '@/theme/use-theme-colors';

/** Mở modal Ghi. Chặn bấm liền nhiều lần trước khi modal kịp mở; nhả khi đường dẫn rời /capture. */
export function useOpenCapture() {
  // Không dùng focus của màn: ô Ghi nằm trên thanh tab, không thuộc màn nào.
  const isOpening = useRef(false);
  const pathname = usePathname();

  useEffect(() => {
    if (pathname !== '/capture') isOpening.current = false;
  }, [pathname]);

  return useCallback(() => {
    if (isOpening.current) return;
    isOpening.current = true;
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    router.push('/capture');
  }, []);
}

/** Icon ô Ghi: cùng khung 56x32 với icon các tab khác (xem TabIcon), chỉ khác là đặc màu nhấn. */
export function RecordTabIcon() {
  const colors = useThemeColors();
  return (
    <View className="h-8 w-14 items-center justify-center rounded-full bg-primary">
      <Icon name="plus" color={colors.paper} strokeWidth={2.5} />
    </View>
  );
}
