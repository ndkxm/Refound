import { router } from 'expo-router';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Icon, type IconName } from '@/components/ui/icon';
import { useThemeColors } from '@/theme/use-theme-colors';

type Method = {
  icon: Required<Pick<IconName, 'android' | 'ios'>>;
  title: string;
  hint: string;
  owner: string;
};

// Nói là cách chính (trụ cột 1), nên đứng riêng ở trên; ba cách còn lại xếp thành danh sách.
const otherMethods: Method[] = [
  {
    icon: { android: 'keyboard', ios: 'keyboard' },
    title: 'Gõ một câu',
    hint: 'Như nhắn tin: "ăn trưa 45k"',
    owner: 'V41 · TV3',
  },
  {
    icon: { android: 'photo_camera', ios: 'camera' },
    title: 'Chụp hoặc chọn ảnh',
    hint: 'Ảnh chuyển khoản, hóa đơn',
    owner: 'V19 · TV4',
  },
  {
    icon: { android: 'edit_note', ios: 'square.and.pencil' },
    title: 'Nhập tay',
    hint: 'Tự điền số tiền và danh mục',
    owner: 'V14 · TV2',
  },
];

function SoonBadge({ inverted = false }: { inverted?: boolean }) {
  return (
    <View
      className={`rounded-full border px-2.5 py-1 ${inverted ? 'border-paper/30' : 'border-line'}`}
    >
      <Text className={`text-[11px] font-semibold ${inverted ? 'text-paper/80' : 'text-muted'}`}>
        Sắp có
      </Text>
    </View>
  );
}

export default function CaptureScreen() {
  const colors = useThemeColors();

  return (
    <SafeAreaView edges={['top', 'bottom']} className="flex-1 bg-paper">
      <View className="flex-row items-center justify-between px-6 pb-2 pt-4">
        <Text className="text-2xl font-bold text-ink" accessibilityRole="header">
          Ghi khoản chi
        </Text>
        <Pressable
          onPress={() => router.back()}
          accessibilityRole="button"
          accessibilityLabel="Đóng"
          hitSlop={12}
          className="h-10 w-10 items-center justify-center rounded-full active:bg-line"
        >
          <Icon name={{ android: 'close', ios: 'xmark' }} color={colors.ink} />
        </Pressable>
      </View>

      <ScrollView contentContainerClassName="px-6 pb-8">
        <Text className="text-base text-muted">Chọn cách nhanh nhất lúc này.</Text>

        <View
          className="mt-6 rounded-3xl bg-ink p-6"
          accessible
          accessibilityLabel="Nói, sắp có. Ví dụ: trà sữa 35 nghìn"
        >
          <View className="flex-row items-start justify-between">
            <View className="h-14 w-14 items-center justify-center rounded-full bg-accent">
              <Icon name={{ android: 'mic', ios: 'mic.fill' }} color={colors.paper} size={28} />
            </View>
            <SoonBadge inverted />
          </View>
          <Text className="mt-6 text-2xl font-bold text-paper">Nói</Text>
          <Text className="mt-1 text-base italic text-paper/70">“trà sữa 35k”</Text>
          <Text className="mt-4 text-xs font-semibold uppercase tracking-widest text-paper/70">
            V38 · TV3
          </Text>
        </View>

        <View className="mt-4">
          {otherMethods.map((method, index) => (
            <View
              key={method.title}
              accessible
              accessibilityLabel={`${method.title}, sắp có`}
              className={`flex-row items-center gap-4 py-4 ${index > 0 ? 'border-t border-line' : ''}`}
            >
              <View className="h-12 w-12 items-center justify-center rounded-full border border-line bg-surface">
                <Icon name={method.icon} color={colors.ink} />
              </View>
              <View className="flex-1">
                <Text className="text-base font-semibold text-ink">{method.title}</Text>
                <Text className="mt-0.5 text-sm text-muted">{method.hint}</Text>
                <Text className="mt-1 text-[11px] font-semibold uppercase tracking-widest text-muted">
                  {method.owner}
                </Text>
              </View>
              <SoonBadge />
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
