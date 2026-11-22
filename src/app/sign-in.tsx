import { Link } from 'expo-router';
import { Text } from 'react-native';

import { PlaceholderScreen } from '@/components/ui/placeholder-screen';
import { useSessionStore } from '@/stores/session-store';

export default function SignInScreen() {
  const signIn = useSessionStore((s) => s.signIn);

  return (
    <PlaceholderScreen
      owner="V08 · TV1"
      title="Đăng nhập"
      description="Form email và mật khẩu sẽ ở đây."
      devActions={[
        { label: 'Vào thử, chưa onboarding', onPress: () => signIn() },
        { label: 'Vào thử, đã onboarding', onPress: () => signIn(true) },
      ]}
    >
      <Link href="/sign-up" className="py-2 text-base text-muted">
        Chưa có tài khoản? <Text className="font-semibold text-ink">Đăng ký</Text>
      </Link>
    </PlaceholderScreen>
  );
}
