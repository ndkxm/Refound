import { api } from '@/lib/api-client';
import type { LoginResult, Me } from '@/types/api';

// Đăng nhập là tìm-hoặc-tạo theo firebase_uid nên gọi lại không sinh trùng; chờ tới 90 s vì server có thể đang thức dậy.
export const loginWithFirebase = (idToken: string) =>
  api<LoginResult>('/auth/firebase', { method: 'POST', body: { idToken }, timeoutMs: 90_000 });

export const getMe = () => api<Me>('/me');

export const updateMe = (body: { displayName: string }) =>
  api<Me>('/me', { method: 'PATCH', body });
