import { ActivityIndicator, Pressable, Text, useColorScheme } from 'react-native';

import { cn } from '@/utils/cn';

import { native } from './tokens';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';

const box: Record<Variant, string> = {
  primary: 'bg-brand-700 active:bg-brand-900 dark:bg-brand-400 dark:active:bg-brand-600',
  secondary:
    'border border-neutral-300 bg-white active:bg-neutral-100 dark:border-neutral-700 dark:bg-neutral-900 dark:active:bg-neutral-800',
  ghost: 'active:bg-neutral-100 dark:active:bg-neutral-800',
  danger: 'bg-danger-700 active:bg-danger-950 dark:bg-danger-400',
};

const label: Record<Variant, string> = {
  primary: 'text-white dark:text-brand-950',
  secondary: 'text-neutral-900 dark:text-neutral-100',
  ghost: 'text-brand-700 dark:text-brand-400',
  danger: 'text-white dark:text-danger-950',
};

type Props = {
  title: string;
  onPress?: () => void;
  variant?: Variant;
  loading?: boolean;
  disabled?: boolean;
  /** Phần tử đứng trước chữ, ví dụ biểu tượng. */
  icon?: React.ReactNode;
  className?: string;
};

export function Button({
  title,
  onPress,
  variant = 'primary',
  loading,
  disabled,
  icon,
  className,
}: Props) {
  const scheme = useColorScheme() === 'dark' ? 'dark' : 'light';
  const inactive = disabled || loading;
  const solid = variant === 'primary' || variant === 'danger';
  const spinner = solid ? native[scheme].onSolid : native[scheme].spinner;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      disabled={inactive}
      onPress={onPress}
      className={cn(
        'min-h-touch flex-row items-center justify-center gap-2 rounded-xl px-5 py-2.5',
        box[variant],
        inactive && 'opacity-50',
        className,
      )}
    >
      {loading ? <ActivityIndicator color={spinner} /> : icon}
      <Text className={cn('text-body font-semibold', label[variant])}>{title}</Text>
    </Pressable>
  );
}
