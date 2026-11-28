import { useId } from 'react';
import { Text, TextInput, useColorScheme, View, type TextInputProps } from 'react-native';

import { cn } from '@/utils/cn';

import { native } from './tokens';

type Props = Omit<TextInputProps, 'className'> & {
  label: string;
  /** Lỗi hiện dưới ô (ví dụ `fieldState.error?.message` của react-hook-form). */
  error?: string;
  hint?: string;
  className?: string;
  ref?: React.Ref<React.ComponentRef<typeof TextInput>>;
};

export function TextField({ label, error, hint, className, ref, ...input }: Props) {
  const scheme = useColorScheme() === 'dark' ? 'dark' : 'light';
  const id = useId();

  return (
    <View className={cn('gap-1', className)}>
      <Text nativeID={id} className="text-label font-medium text-neutral-900 dark:text-neutral-100">
        {label}
      </Text>
      <TextInput
        ref={ref}
        accessibilityLabelledBy={id}
        accessibilityHint={error ?? hint}
        placeholderTextColor={native[scheme].placeholder}
        className={cn(
          'min-h-touch rounded-xl border bg-white px-4 py-2.5 text-body text-neutral-900 dark:bg-neutral-900 dark:text-neutral-100',
          error
            ? 'border-danger-700 dark:border-danger-400'
            : 'border-neutral-300 dark:border-neutral-700',
        )}
        {...input}
      />
      {error ? (
        <Text
          accessibilityLiveRegion="polite"
          className="text-caption text-danger-700 dark:text-danger-400"
        >
          {error}
        </Text>
      ) : hint ? (
        <Text className="text-caption text-neutral-500 dark:text-neutral-400">{hint}</Text>
      ) : null}
    </View>
  );
}
