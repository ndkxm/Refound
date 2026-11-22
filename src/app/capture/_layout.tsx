import { Stack } from 'expo-router';

// Modal Ghi: V38 (giọng nói), V41 (câu gõ), V19 (ảnh) thêm màn vào stack này.
export default function CaptureLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
