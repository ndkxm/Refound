export type SessionStatus = 'loading' | 'signed-out' | 'signed-in';

export type RouteAccess = {
  /** false khi chưa biết phiên đăng nhập: giữ splash, chưa mở màn nào. */
  isReady: boolean;
  auth: boolean;
  onboarding: boolean;
  app: boolean;
};

/** Nhóm màn nào được mở theo trạng thái phiên. Mỗi lúc chỉ đúng một nhóm (hoặc không nhóm nào). */
export function getRouteAccess(session: {
  status: SessionStatus;
  hasOnboarded: boolean;
}): RouteAccess {
  const signedIn = session.status === 'signed-in';
  return {
    isReady: session.status !== 'loading',
    auth: session.status === 'signed-out',
    onboarding: signedIn && !session.hasOnboarded,
    app: signedIn && session.hasOnboarded,
  };
}
