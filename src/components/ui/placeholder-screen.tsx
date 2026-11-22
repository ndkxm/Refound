import type { ReactNode } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Icon, type IconName } from '@/components/ui/icon';
import { useThemeColors } from '@/theme/use-theme-colors';

type DevAction = { label: string; onPress: () => void };

type Props = {
  title: string;
  description: string;
  /** Việc và người làm màn thật, ví dụ "V25 · TV5". */
  owner: string;
  /** Icon của màn, cùng icon với tab tương ứng. */
  icon: IconName;
  /** Nút chỉ hiện khi chạy dev, để đi thử luồng trước khi có màn thật. */
  devActions?: DevAction[];
  children?: ReactNode;
};

/** Màn tạm có chủ đích: nói rõ màn này sẽ có gì và ai làm. Thay bằng màn thật khi làm việc tương ứng. */
export function PlaceholderScreen({
  title,
  description,
  owner,
  icon,
  devActions = [],
  children,
}: Props) {
  const colors = useThemeColors();

  return (
    <SafeAreaView edges={['top']} className="flex-1 bg-paper">
      <ScrollView contentContainerClassName="grow px-5 pb-10 pt-6">
        <Text className="text-3xl font-bold text-ink" accessibilityRole="header">
          {title}
        </Text>
        <Text className="mt-2 text-base leading-6 text-muted">{description}</Text>

        <View className="mt-6 flex-row items-center gap-4 rounded-3xl border border-line bg-surface p-4">
          <View className="h-12 w-12 items-center justify-center rounded-2xl bg-primary/10">
            <Icon name={icon} color={colors.primary} />
          </View>
          <View className="flex-1">
            <Text className="text-base font-semibold text-ink">Màn này sắp có</Text>
            <Text className="mt-0.5 text-sm text-muted">Việc {owner}</Text>
          </View>
        </View>

        {children ? <View className="mt-4">{children}</View> : null}

        {__DEV__ && devActions.length > 0 ? (
          <View className="mt-auto gap-3 pt-10">
            <Text className="text-sm text-muted">Chỉ hiện khi chạy dev</Text>
            {devActions.map((action, index) => (
              <Pressable
                key={action.label}
                onPress={action.onPress}
                accessibilityRole="button"
                className={`min-h-12 items-center justify-center rounded-full px-5 ${
                  index === 0
                    ? 'bg-primary active:opacity-80'
                    : 'border border-line bg-surface active:bg-line'
                }`}
              >
                <Text
                  className={`text-base font-semibold ${index === 0 ? 'text-paper' : 'text-ink'}`}
                >
                  {action.label}
                </Text>
              </Pressable>
            ))}
          </View>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}
