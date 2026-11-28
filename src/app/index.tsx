import { Text } from 'react-native';

import { Button, Screen } from '@/components/ui';
import { signOut } from '@/features/auth/hooks/use-auth';
import { useAuthStore } from '@/stores/auth-store';

// Màn tạm sau đăng nhập; P0-08 thay bằng các tab (bản đồ, tìm kiếm, đăng tin…).
export default function Home() {
  const name = useAuthStore((s) => s.user?.displayName);

  return (
    <Screen className="items-center justify-center gap-4 px-6">
      <Text className="text-title font-semibold text-neutral-900 dark:text-neutral-100">
        Xin chào, {name}
      </Text>
      <Button title="Đăng xuất" variant="secondary" onPress={() => void signOut()} />
    </Screen>
  );
}
