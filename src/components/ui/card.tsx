import { Pressable, View } from 'react-native';

import { cn } from '@/utils/cn';

type Props = {
  children: React.ReactNode;
  /** Có `onPress` thì cả thẻ là một nút; cần `accessibilityLabel` mô tả thẻ. */
  onPress?: () => void;
  accessibilityLabel?: string;
  className?: string;
};

const base =
  'rounded-2xl border border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-900';

export function Card({ children, onPress, accessibilityLabel, className }: Props) {
  if (!onPress) return <View className={cn(base, className)}>{children}</View>;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel}
      onPress={onPress}
      className={cn(
        base,
        'min-h-touch active:bg-neutral-100 dark:active:bg-neutral-800',
        className,
      )}
    >
      {children}
    </Pressable>
  );
}
