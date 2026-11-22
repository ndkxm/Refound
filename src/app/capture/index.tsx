import { router } from 'expo-router';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Icon, type IconName } from '@/components/ui/icon';
import { useThemeColors } from '@/theme/use-theme-colors';

type Method = {
  icon: IconName;
  title: string;
  hint: string;
  owner: string;
};

// Nói là cách chính (trụ cột 1), nên đứng riêng ở trên; ba cách còn lại xếp thành danh sách.
const otherMethods: Method[] = [
  {
    icon: 'keyboard',
    title: 'Gõ một câu',
    hint: 'Như nhắn tin: “ăn trưa 45k”',
    owner: 'V41 · TV3',
  },
  {
    icon: 'camera',
    title: 'Chụp hoặc chọn ảnh',
    hint: 'Ảnh chuyển khoản, hóa đơn',
    owner: 'V19 · TV4',
  },
  { icon: 'pencil', title: 'Nhập tay', hint: 'Tự điền số tiền và danh mục', owner: 'V14 · TV2' },
];

export default function CaptureScreen() {
  const colors = useThemeColors();

  return (
    <SafeAreaView edges={['top', 'bottom']} className="flex-1 bg-paper">
      <View className="flex-row items-center justify-between px-5 pb-1 pt-3">
        <Text className="text-2xl font-bold text-ink" accessibilityRole="header">
          Ghi khoản chi
        </Text>
        <Pressable
          onPress={() => router.back()}
          accessibilityRole="button"
          accessibilityLabel="Đóng"
          hitSlop={8}
          className="h-12 w-12 items-center justify-center rounded-full border border-line bg-surface active:bg-line"
        >
          <Icon name="x" color={colors.ink} size={22} />
        </Pressable>
      </View>

      <ScrollView contentContainerClassName="px-5 pb-8 pt-2">
        <Text className="text-base text-muted">Chọn cách nhanh nhất lúc này.</Text>

        {/* Điểm nhấn duy nhất của màn: cách ghi chính, đặc màu primary. */}
        <View
          className="mt-5 rounded-3xl bg-primary p-5"
          accessible
          accessibilityLabel="Nói, sắp có. Ví dụ: trà sữa 35 nghìn"
        >
          <View className="flex-row items-start justify-between">
            <View className="h-14 w-14 items-center justify-center rounded-full bg-paper/20">
              <Icon name="mic" color={colors.paper} size={28} />
            </View>
            <View className="rounded-full bg-paper/20 px-3 py-1">
              <Text className="text-xs font-semibold text-paper">Sắp có</Text>
            </View>
          </View>
          <Text className="mt-6 text-3xl font-bold text-paper">Nói</Text>
          <Text className="mt-1 text-base text-paper">Ví dụ: “trà sữa 35k”</Text>
          <Text className="mt-3 text-sm text-paper/80">Việc V38 · TV3</Text>
        </View>

        <View className="mt-3 overflow-hidden rounded-3xl border border-line bg-surface">
          {otherMethods.map((method, index) => (
            <View
              key={method.title}
              accessible
              accessibilityLabel={`${method.title}, sắp có`}
              className={`flex-row items-center gap-4 px-4 py-4 ${index > 0 ? 'border-t border-line' : ''}`}
            >
              <View className="h-12 w-12 items-center justify-center rounded-2xl bg-primary/10">
                <Icon name={method.icon} color={colors.primary} />
              </View>
              <View className="flex-1">
                <Text className="text-base font-semibold text-ink">{method.title}</Text>
                <Text className="mt-0.5 text-sm text-muted">{method.hint}</Text>
                <Text className="mt-1 text-sm text-muted">Việc {method.owner}</Text>
              </View>
              <Text className="text-sm font-semibold text-muted">Sắp có</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
