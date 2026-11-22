import { create } from 'zustand';

import type { SessionStatus } from '@/features/auth/utils/route-access';

type SessionState = {
  status: SessionStatus;
  hasOnboarded: boolean;
  /** V08 gọi khi Supabase Auth báo đã đăng nhập; truyền true nếu profiles đã onboarding. */
  signIn: (hasOnboarded?: boolean) => void;
  signOut: () => void;
  /** V09 gọi sau khi lưu xong profiles. */
  completeOnboarding: () => void;
};

// shortcut: khởi đầu là 'signed-out' vì chưa có Supabase; V08 đổi thành 'loading' rồi khôi phục phiên.
export const useSessionStore = create<SessionState>()((set) => ({
  status: 'signed-out',
  hasOnboarded: false,
  signIn: (hasOnboarded = false) => set({ status: 'signed-in', hasOnboarded }),
  signOut: () => set({ status: 'signed-out', hasOnboarded: false }),
  completeOnboarding: () => set({ hasOnboarded: true }),
}));
