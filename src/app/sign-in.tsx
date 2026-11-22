import { Link } from 'expo-router';
import { Pressable, Text } from 'react-native';

import { PlaceholderScreen } from '@/components/ui/placeholder-screen';
import { useSessionStore } from '@/stores/session-store';

export default function SignInScreen() {
  const signIn = useSessionStore((s) => s.signIn);

  return (
    <PlaceholderScreen
      icon="user"
      owner="V08 · TV1"
      title="Đăng nhập"
      description="Form email và mật khẩu sẽ ở đây."
      devActions={[
        { label: 'Vào thử, chưa onboarding', onPress: () => signIn() },
        { label: 'Vào thử, đã onboarding', onPress: () => signIn(true) },
      ]}
    >
      <Link href="/sign-up" asChild>
        <Pressable accessibilityRole="link" className="min-h-12 items-center justify-center">
          <Text className="text-base text-ink">
            Chưa có tài khoản? <Text className="font-bold text-primary underline">Đăng ký</Text>
          </Text>
        </Pressable>
      </Link>
    </PlaceholderScreen>
  );
}
