import { ActivityIndicator, Text, useColorScheme, View } from 'react-native';
import { useSafeAreaInsets, type Edge } from 'react-native-safe-area-context';

import { cn } from '@/utils/cn';

import { Button } from './button';
import { native } from './tokens';

type Props = {
  children?: React.ReactNode;
  /** Ưu tiên: loading → error → empty → children. */
  loading?: boolean;
  /** Lỗi của query; hiện `error.message` (ApiError đã mang câu tiếng Việt). */
  error?: Error | null;
  onRetry?: () => void;
  empty?: boolean;
  emptyTitle?: string;
  emptyHint?: string;
  /** Nút gợi ý khi rỗng, ví dụ "Đăng tin". */
  emptyAction?: React.ReactNode;
  /** Màn trong tab hoặc có header thì bỏ cạnh trên. */
  edges?: Edge[];
  className?: string;
};

export function Screen({
  children,
  loading,
  error,
  onRetry,
  empty,
  emptyTitle = 'Chưa có gì ở đây',
  emptyHint,
  emptyAction,
  edges = ['top', 'bottom', 'left', 'right'],
  className,
}: Props) {
  const scheme = useColorScheme() === 'dark' ? 'dark' : 'light';
  const insets = useSafeAreaInsets();

  let body: React.ReactNode = children;
  if (loading) {
    body = (
      <Centered>
        <ActivityIndicator size="large" color={native[scheme].spinner} />
        <Text className="text-body text-neutral-500 dark:text-neutral-400">Đang tải…</Text>
      </Centered>
    );
  } else if (error) {
    body = (
      <Centered>
        <Text
          accessibilityRole="alert"
          className="text-center text-body text-neutral-900 dark:text-neutral-100"
        >
          {error.message || 'Đã có lỗi xảy ra.'}
        </Text>
        {onRetry ? <Button title="Thử lại" variant="secondary" onPress={onRetry} /> : null}
      </Centered>
    );
  } else if (empty) {
    body = (
      <Centered>
        <Text className="text-center text-title font-semibold text-neutral-900 dark:text-neutral-100">
          {emptyTitle}
        </Text>
        {emptyHint ? (
          <Text className="text-center text-body text-neutral-500 dark:text-neutral-400">
            {emptyHint}
          </Text>
        ) : null}
        {emptyAction}
      </Centered>
    );
  }

  // View + insets thay vì SafeAreaView: className chỉ tự chạy trên component lõi của React Native.
  const pad = (edge: Edge, value: number) => (edges.includes(edge) ? value : 0);
  return (
    <View
      className={cn('flex-1 bg-neutral-50 dark:bg-neutral-950', className)}
      style={{
        paddingTop: pad('top', insets.top),
        paddingBottom: pad('bottom', insets.bottom),
        paddingLeft: pad('left', insets.left),
        paddingRight: pad('right', insets.right),
      }}
    >
      {body}
    </View>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return <View className="flex-1 items-center justify-center gap-4 px-6">{children}</View>;
}
