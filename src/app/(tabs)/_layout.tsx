import { router, Tabs } from 'expo-router';

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="index" options={{ title: 'Hôm nay' }} />
      <Tabs.Screen name="transactions" options={{ title: 'Sổ' }} />
      <Tabs.Screen
        name="record"
        options={{ title: 'Ghi' }}
        listeners={{
          // Ô Ghi không phải một tab: bấm vào thì mở modal Ghi.
          tabPress: (e) => {
            e.preventDefault();
            router.push('/capture');
          },
        }}
      />
      <Tabs.Screen name="map" options={{ title: 'Bản đồ' }} />
      <Tabs.Screen name="groups" options={{ title: 'Nhóm' }} />
    </Tabs>
  );
}
