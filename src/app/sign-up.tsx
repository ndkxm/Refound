import { Link } from 'expo-router';
import { Pressable, Text } from 'react-native';

import { PlaceholderScreen } from '@/components/ui/placeholder-screen';

export default function SignUpScreen() {
  return (
    <PlaceholderScreen
      icon="user"
      owner="V08 · TV1"
      title="Đăng ký"
      description="Tạo tài khoản bằng email và mật khẩu."
    >
      <Link href="/sign-in" dismissTo asChild>
        <Pressable accessibilityRole="link" className="min-h-12 items-center justify-center">
          <Text className="text-base text-ink">
            Đã có tài khoản? <Text className="font-bold text-primary underline">Đăng nhập</Text>
          </Text>
        </Pressable>
      </Link>
    </PlaceholderScreen>
  );
}
