import { getRouteAccess, type SessionStatus } from './route-access';

describe('getRouteAccess', () => {
  it.each<[SessionStatus, boolean, ReturnType<typeof getRouteAccess>]>([
    ['loading', false, { isReady: false, auth: false, onboarding: false, app: false }],
    ['signed-out', false, { isReady: true, auth: true, onboarding: false, app: false }],
    ['signed-in', false, { isReady: true, auth: false, onboarding: true, app: false }],
    ['signed-in', true, { isReady: true, auth: false, onboarding: false, app: true }],
  ])('%s, đã onboarding = %s', (status, hasOnboarded, expected) => {
    expect(getRouteAccess({ status, hasOnboarded })).toEqual(expected);
  });

  it('chưa đăng nhập thì chỉ vào được màn đăng nhập, kể cả khi cờ onboarding còn sót', () => {
    expect(getRouteAccess({ status: 'signed-out', hasOnboarded: true })).toEqual({
      isReady: true,
      auth: true,
      onboarding: false,
      app: false,
    });
  });

  it('đang tải phiên thì không mở màn nào, kể cả khi cờ onboarding đã bật', () => {
    expect(getRouteAccess({ status: 'loading', hasOnboarded: true }).app).toBe(false);
  });
});
