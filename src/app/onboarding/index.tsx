import { PlaceholderScreen } from '@/components/ui/placeholder-screen';
import { useSessionStore } from '@/stores/session-store';

export default function OnboardingScreen() {
  const completeOnboarding = useSessionStore((s) => s.completeOnboarding);
  const signOut = useSessionStore((s) => s.signOut);

  return (
    <PlaceholderScreen
      icon="wallet"
      owner="V09 · TV1"
      title="Thiết lập tài chính"
      description="Bốn bước ngắn: cách nhận thu nhập, ngày nhận tiền, các khoản cố định và số tiền muốn để dành mỗi tháng."
      devActions={[
        { label: 'Hoàn tất onboarding', onPress: completeOnboarding },
        { label: 'Đăng xuất', onPress: signOut },
      ]}
    />
  );
}
