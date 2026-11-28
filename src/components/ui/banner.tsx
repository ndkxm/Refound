import { Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

/** Dải thông báo nổi ở đáy màn hình, ví dụ "Máy chủ đang khởi động". */
export function Banner({ message }: { message: string }) {
  const insets = useSafeAreaInsets();
  return (
    <View
      accessibilityRole="alert"
      accessibilityLiveRegion="polite"
      pointerEvents="none"
      className="absolute inset-x-0 bottom-0 bg-warn-100 px-4 pt-3 dark:bg-warn-950"
      style={{ paddingBottom: insets.bottom + 12 }}
    >
      <Text className="text-center text-label text-warn-900 dark:text-warn-100">{message}</Text>
    </View>
  );
}
