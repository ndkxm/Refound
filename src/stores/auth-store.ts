import * as SecureStore from 'expo-secure-store';
import { create } from 'zustand';

import type { Me } from '@/types/api';

const TOKEN_KEY = 'refound.jwt';
const USER_KEY = 'refound.me';

type State = {
  /** `loading` cho tới khi đọc xong secure-store lúc mở app. */
  status: 'loading' | 'signedOut' | 'signedIn';
  token: string | null;
  user: Me | null;
  /** Tài khoản mới (`isNew`): phải qua màn đặt tên trước khi vào app. */
  needsProfile: boolean;
  hydrate: () => Promise<void>;
  signIn: (token: string, user: Me, isNew: boolean) => Promise<void>;
  setUser: (user: Me) => Promise<void>;
  profileDone: () => void;
  signOut: () => Promise<void>;
};

// JWT 7 ngày, không có refresh token (ADR-05): hết hạn thì api-client gặp 401 và gọi signOut.
export const useAuthStore = create<State>((set) => ({
  status: 'loading',
  token: null,
  user: null,
  needsProfile: false,

  hydrate: async () => {
    try {
      const [token, user] = await Promise.all([
        SecureStore.getItemAsync(TOKEN_KEY),
        SecureStore.getItemAsync(USER_KEY),
      ]);
      if (token && user) {
        set({ status: 'signedIn', token, user: JSON.parse(user) as Me });
        return;
      }
    } catch {
      // Keystore hỏng (cài lại app, đổi khóa màn hình): coi như chưa đăng nhập.
    }
    set({ status: 'signedOut', token: null, user: null });
  },

  signIn: async (token, user, isNew) => {
    await Promise.all([
      SecureStore.setItemAsync(TOKEN_KEY, token),
      SecureStore.setItemAsync(USER_KEY, JSON.stringify(user)),
    ]);
    set({ status: 'signedIn', token, user, needsProfile: isNew });
  },

  setUser: async (user) => {
    await SecureStore.setItemAsync(USER_KEY, JSON.stringify(user));
    set({ user });
  },

  profileDone: () => set({ needsProfile: false }),

  signOut: async () => {
    set({ status: 'signedOut', token: null, user: null, needsProfile: false });
    await Promise.all([
      SecureStore.deleteItemAsync(TOKEN_KEY),
      SecureStore.deleteItemAsync(USER_KEY),
    ]);
  },
}));
