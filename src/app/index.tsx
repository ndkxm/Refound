import { Text, View } from 'react-native';

// Màn tạm để kiểm tra NativeWind chạy được.
export default function Index() {
  return (
    <View className="flex-1 items-center justify-center gap-2 bg-white px-6 dark:bg-neutral-950">
      <Text className="text-3xl font-bold text-emerald-600">Refound</Text>
      <Text className="text-sm text-neutral-400">Khung dự án</Text>
    </View>
  );
}
