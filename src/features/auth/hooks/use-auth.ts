import { useMutation } from '@tanstack/react-query';

import { queryClient } from '@/lib/query-client';
import { useAuthStore } from '@/stores/auth-store';

import { loginWithFirebase, updateMe } from '../api/auth-api';
import { getFirebaseIdToken, signOutGoogle } from '../api/google-sign-in';

/** Nút "Đăng nhập bằng Google". Kết quả `false` khi người dùng tự hủy. */
export function useGoogleLogin() {
  return useMutation({
    mutationFn: async () => {
      const idToken = await getFirebaseIdToken();
      if (!idToken) return false;
      const { token, user, isNew } = await loginWithFirebase(idToken);
      await useAuthStore.getState().signIn(token, user, isNew);
      return true;
    },
  });
}

export function useUpdateProfile() {
  return useMutation({
    mutationFn: updateMe,
    onSuccess: (me) => useAuthStore.getState().setUser(me),
  });
}

export async function signOut() {
  await signOutGoogle();
  queryClient.clear();
  await useAuthStore.getState().signOut();
}
