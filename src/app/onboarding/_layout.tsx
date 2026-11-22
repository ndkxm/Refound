import { Stack } from 'expo-router';

// V09 thêm các bước: /onboarding/<bước>.
export default function OnboardingLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
