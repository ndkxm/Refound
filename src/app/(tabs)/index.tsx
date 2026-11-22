import { PlaceholderScreen } from '@/components/ui/placeholder-screen';
import { useSessionStore } from '@/stores/session-store';

export default function TodayScreen() {
  const signOut = useSessionStore((s) => s.signOut);

  return (
    <PlaceholderScreen
      icon="home"
      owner="V25 · TV5"
      title="Hôm nay"
      description="Số tiền còn được tiêu hôm nay, các khoản đã ghi trong ngày và nút mic để ghi nhanh."
      devActions={[{ label: 'Đăng xuất', onPress: signOut }]}
    />
  );
}
