import { Stack } from 'expo-router';
import { act, fireEvent, renderRouter, screen, testRouter } from 'expo-router/testing-library';
import { Text } from 'react-native';

import TabsLayout from '@/app/(tabs)/_layout';

function page(name: string) {
  return function Page() {
    return <Text>{name}</Text>;
  };
}

const routes = {
  _layout: () => <Stack />,
  '(tabs)/_layout': TabsLayout,
  '(tabs)/index': page('home'),
  '(tabs)/transactions/index': page('transactions'),
  '(tabs)/record': page('record'),
  '(tabs)/map': page('map'),
  '(tabs)/groups/index': page('groups'),
  'capture/index': page('capture'),
};

describe('nút Ghi trên thanh tab', () => {
  it('mở modal /capture thay vì chuyển sang tab Ghi', async () => {
    await renderRouter(routes);
    await act(async () => {
      fireEvent.press(screen.getByLabelText('Ghi khoản chi'));
    });
    expect(screen).toHavePathname('/capture');
  });

  it('đóng modal rồi bấm lại thì vẫn mở được', async () => {
    await renderRouter(routes);
    await act(async () => {
      fireEvent.press(screen.getByLabelText('Ghi khoản chi'));
    });
    await testRouter.back('/');
    await act(async () => {
      fireEvent.press(screen.getByLabelText('Ghi khoản chi'));
    });
    expect(screen).toHavePathname('/capture');
  });

  it('bấm liền 3 lần chỉ mở một modal', async () => {
    const r = await renderRouter(routes);
    const button = screen.getByLabelText('Ghi khoản chi');
    // Ba lần bấm trước khi màn kịp vẽ lại, như khi người dùng bấm vội.
    await act(async () => {
      fireEvent.press(button);
      fireEvent.press(button);
      fireEvent.press(button);
    });

    const rootRoutes = r.getRouterState()?.routes[0]?.state?.routes ?? [];
    expect(rootRoutes.filter((route) => route.name === 'capture/index')).toHaveLength(1);
  });
});
