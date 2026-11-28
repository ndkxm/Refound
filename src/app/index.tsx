import { Text } from 'react-native';

import { Screen } from '@/components/ui';

// Màn tạm, P0-03 thay bằng khung điều hướng.
export default function Index() {
  return (
    <Screen className="items-center justify-center gap-2 px-6">
      <Text className="text-display font-bold text-brand-700 dark:text-brand-400">Refound</Text>
      <Text className="text-label text-neutral-500 dark:text-neutral-400">Khung dự án</Text>
    </Screen>
  );
}
