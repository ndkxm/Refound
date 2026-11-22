import { useSessionStore } from './session-store';

const initial = useSessionStore.getState();

beforeEach(() => {
  useSessionStore.setState(initial, true);
});

describe('useSessionStore', () => {
  it('mở app khi chưa có Supabase thì là chưa đăng nhập', () => {
    expect(useSessionStore.getState()).toMatchObject({ status: 'signed-out', hasOnboarded: false });
  });

  it('signIn() mặc định là chưa onboarding', () => {
    useSessionStore.getState().signIn();
    expect(useSessionStore.getState()).toMatchObject({ status: 'signed-in', hasOnboarded: false });
  });

  it('signIn(true) dành cho tài khoản đã onboarding từ trước', () => {
    useSessionStore.getState().signIn(true);
    expect(useSessionStore.getState()).toMatchObject({ status: 'signed-in', hasOnboarded: true });
  });

  it('completeOnboarding() bật cờ onboarding', () => {
    useSessionStore.getState().signIn();
    useSessionStore.getState().completeOnboarding();
    expect(useSessionStore.getState().hasOnboarded).toBe(true);
  });

  it('signOut() xóa cờ onboarding để tài khoản sau không vào thẳng app', () => {
    useSessionStore.getState().signIn(true);
    useSessionStore.getState().signOut();
    expect(useSessionStore.getState()).toMatchObject({ status: 'signed-out', hasOnboarded: false });
  });
});
