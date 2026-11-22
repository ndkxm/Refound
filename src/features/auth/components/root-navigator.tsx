import { Stack } from 'expo-router';

import { getRouteAccess } from '@/features/auth/utils/route-access';
import { useSessionStore } from '@/stores/session-store';

/**
 * Stack gốc: mỗi nhóm màn chỉ mở được khi guard của nó đúng.
 * Guard đổi thì Expo Router tự chuyển màn và xóa lịch sử của nhóm vừa bị khóa.
 */
export default function RootNavigator() {
  const status = useSessionStore((s) => s.status);
  const hasOnboarded = useSessionStore((s) => s.hasOnboarded);
  const access = getRouteAccess({ status, hasOnboarded });

  // Đang khôi phục phiên: root layout giữ splash, chưa render màn nào.
  if (!access.isReady) return null;

  return (
    <Stack screenOptions={{ headerShown: false }}>
      {/* sign-in ở gốc (không bọc nhóm): khi bị khóa, router chuyển về màn đầu tiên được phép,
          nên màn đó phải là một route thật chứ không phải một nhóm không có index. */}
      <Stack.Protected guard={access.auth}>
        <Stack.Screen name="sign-in" />
        <Stack.Screen name="sign-up" />
      </Stack.Protected>
      <Stack.Protected guard={access.onboarding}>
        <Stack.Screen name="onboarding" />
      </Stack.Protected>
      <Stack.Protected guard={access.app}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="capture" options={{ presentation: 'modal' }} />
      </Stack.Protected>
    </Stack>
  );
}
