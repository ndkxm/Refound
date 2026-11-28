import { Text, View } from 'react-native';

import { Button, Screen } from '@/components/ui';
import { useGoogleLogin } from '@/features/auth/hooks/use-auth';

export default function Login() {
  const login = useGoogleLogin();

  return (
    <Screen className="justify-between px-6 py-10">
      <View className="flex-1 items-center justify-center gap-3">
        <Text className="text-display font-bold text-brand-700 dark:text-brand-400">Refound</Text>
        <Text className="text-center text-body text-neutral-500 dark:text-neutral-400">
          Tìm lại đồ thất lạc trong trường
        </Text>
      </View>
      <View className="gap-3">
        {login.error ? (
          <Text
            accessibilityRole="alert"
            className="text-center text-label text-danger-700 dark:text-danger-400"
          >
            {login.error.message}
          </Text>
        ) : null}
        <Button
          title={login.error ? 'Thử lại' : 'Đăng nhập bằng Google'}
          loading={login.isPending}
          onPress={() => login.mutate()}
        />
      </View>
    </Screen>
  );
}
