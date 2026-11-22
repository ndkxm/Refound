import type { ReactNode } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

type DevAction = { label: string; onPress: () => void };

type Props = {
  title: string;
  description: string;
  /** Việc và người làm màn thật, ví dụ "V25 · TV5". */
  owner: string;
  /** Nút chỉ hiện khi chạy dev, để đi thử luồng trước khi có màn thật. */
  devActions?: DevAction[];
  children?: ReactNode;
};

/** Màn tạm có chủ đích: nói rõ màn này sẽ có gì và ai làm. Thay bằng màn thật khi làm việc tương ứng. */
export function PlaceholderScreen({ title, description, owner, devActions = [], children }: Props) {
  return (
    <SafeAreaView edges={['top']} className="flex-1 bg-paper">
      <ScrollView contentContainerClassName="grow px-6 pb-10 pt-10">
        <Text className="text-xs font-semibold uppercase tracking-widest text-muted">{owner}</Text>
        <Text className="mt-3 text-4xl font-bold text-ink" accessibilityRole="header">
          {title}
        </Text>
        <Text className="mt-3 text-base leading-6 text-muted">{description}</Text>

        {/* Dòng kẻ như trang sổ còn trống. */}
        <View className="mt-8 gap-7" accessibilityElementsHidden importantForAccessibility="no">
          {[0, 1, 2, 3].map((i) => (
            <View key={i} className="h-px bg-line" />
          ))}
        </View>

        {children ? <View className="mt-8">{children}</View> : null}

        {__DEV__ && devActions.length > 0 ? (
          <View className="mt-auto gap-3 pt-10">
            <Text className="text-xs text-muted">Chỉ hiện khi chạy dev</Text>
            {devActions.map((action) => (
              <Pressable
                key={action.label}
                onPress={action.onPress}
                accessibilityRole="button"
                className="min-h-12 items-center justify-center rounded-full border border-line px-5 active:bg-line"
              >
                <Text className="text-base font-medium text-ink">{action.label}</Text>
              </Pressable>
            ))}
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
