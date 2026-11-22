import { Stack, Tabs } from 'expo-router';
import { act, renderRouter, screen, testRouter } from 'expo-router/testing-library';
import { Text } from 'react-native';

import { useSessionStore } from '@/stores/session-store';

import RootNavigator from './root-navigator';

function page(name: string) {
  return function Page() {
    return <Text>{name}</Text>;
  };
}

// Giống cây src/app: thư mục nào có _layout thì ở đây cũng có, để Stack.Screen name="…" trỏ đúng.
const routes = {
  _layout: RootNavigator,
  'sign-in': page('sign-in'),
  'sign-up': page('sign-up'),
  'onboarding/_layout': () => <Stack />,
  'onboarding/index': page('onboarding'),
  '(tabs)/_layout': () => <Tabs />,
  '(tabs)/index': page('home'),
  '(tabs)/map': page('map'),
  'capture/_layout': () => <Stack />,
  'capture/index': page('capture'),
};

const initial = useSessionStore.getState();

beforeEach(() => {
  useSessionStore.setState(initial, true);
});

describe('RootNavigator', () => {
  it('chưa đăng nhập thì vào màn đăng nhập', async () => {
    await renderRouter(routes);
    expect(screen).toHavePathname('/sign-in');
  });

  it('đăng nhập rồi thì sang onboarding, xong onboarding thì vào Hôm nay', async () => {
    await renderRouter(routes);

    await act(async () => {
      useSessionStore.getState().signIn();
    });
    expect(screen).toHavePathname('/onboarding');

    await act(async () => {
      useSessionStore.getState().completeOnboarding();
    });
    expect(screen).toHavePathname('/');
  });

  it('đã xong cả hai thì mở thẳng được link /map', async () => {
    useSessionStore.getState().signIn(true);
    await renderRouter(routes, { initialUrl: '/map' });
    expect(screen).toHavePathname('/map');
  });

  it('chưa đăng nhập mà mở link /map thì về màn đăng nhập', async () => {
    await renderRouter(routes, { initialUrl: '/map' });
    expect(screen).toHavePathname('/sign-in');
  });

  it('đăng xuất khi đang mở modal Ghi thì về đăng nhập và không back lại được', async () => {
    useSessionStore.getState().signIn(true);
    await renderRouter(routes);
    await testRouter.push('/capture');

    await act(async () => {
      useSessionStore.getState().signOut();
    });

    expect(screen).toHavePathname('/sign-in');
    expect(testRouter.canGoBack()).toBe(false);
  });

  it('đang tải phiên thì chưa hiện màn nào', async () => {
    useSessionStore.setState({ status: 'loading' });
    await renderRouter(routes);
    expect(screen.queryByText('sign-in')).toBeNull();
    expect(screen.queryByText('home')).toBeNull();
  });
});
