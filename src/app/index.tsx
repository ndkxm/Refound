import { Text, View } from 'react-native';

import { formatVnd } from '@/utils/money';

// Màn tạm để kiểm tra NativeWind chạy được. Màn Home thật là việc V25.
export default function Index() {
  return (
    <View className="flex-1 items-center justify-center gap-2 bg-white px-6 dark:bg-neutral-950">
      <Text className="text-base text-neutral-500">Hôm nay còn được tiêu</Text>
      <Text className="text-5xl font-bold text-emerald-600">{formatVnd(81000)}</Text>
      <Text className="mt-6 text-sm text-neutral-400">Ví Lười · khung dự án</Text>
    </View>
  );
}
