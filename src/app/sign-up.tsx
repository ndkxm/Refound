import { Link } from 'expo-router';
import { Text } from 'react-native';

import { PlaceholderScreen } from '@/components/ui/placeholder-screen';

export default function SignUpScreen() {
  return (
    <PlaceholderScreen
      owner="V08 · TV1"
      title="Đăng ký"
      description="Tạo tài khoản bằng email và mật khẩu."
    >
      <Link href="/sign-in" dismissTo className="text-muted py-2 text-base">
        Đã có tài khoản? <Text className="text-ink font-semibold">Đăng nhập</Text>
      </Link>
    </PlaceholderScreen>
  );
}
