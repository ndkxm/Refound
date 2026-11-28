import { useAuthStore } from '@/stores/auth-store';

import { ApiError, NETWORK_ERROR, TIMEOUT } from './api-error';

const DEFAULT_TIMEOUT_MS = 30_000;

export const apiBaseUrl = () => `${process.env.EXPO_PUBLIC_API_URL ?? ''}/api/v1`;

type Options = {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  /** Đối tượng thì gửi JSON; `FormData` thì gửi multipart (fetch tự đặt boundary). */
  body?: unknown;
  timeoutMs?: number;
  signal?: AbortSignal;
};

/**
 * Gọi REST của server: gắn JWT, hết giờ sau 30 s, lỗi HTTP thành {@link ApiError} mang `detail` tiếng Việt.
 * 401 ngoài `/auth/*` nghĩa là JWT hết hạn hoặc tài khoản bị khóa → đăng xuất.
 */
export async function api<T>(
  path: string,
  { method = 'GET', body, timeoutMs = DEFAULT_TIMEOUT_MS, signal }: Options = {},
): Promise<T> {
  const token = useAuthStore.getState().token;
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';

  const timeout = new AbortController();
  const timer = setTimeout(() => timeout.abort(), timeoutMs);
  signal?.addEventListener('abort', () => timeout.abort());

  let res: Response;
  try {
    res = await fetch(apiBaseUrl() + path, {
      method,
      headers,
      body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
      signal: timeout.signal,
    });
  } catch {
    if (signal?.aborted) throw new ApiError(0, 'ABORTED', 'Đã hủy.');
    if (timeout.signal.aborted)
      throw new ApiError(0, TIMEOUT, 'Máy chủ phản hồi quá lâu. Thử lại sau.');
    throw new ApiError(0, NETWORK_ERROR, 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
  } finally {
    clearTimeout(timer);
  }

  if (res.ok) {
    return (res.status === 204 ? undefined : await res.json()) as T;
  }

  const problem = (await res.json().catch(() => ({}))) as {
    code?: string;
    detail?: string;
    errors?: { field: string; message: string }[];
    retryAfterSeconds?: number;
  };
  // So token: phản hồi muộn của phiên cũ không được đăng xuất phiên vừa đăng nhập lại.
  if (
    res.status === 401 &&
    token &&
    token === useAuthStore.getState().token &&
    !path.startsWith('/auth/')
  ) {
    void useAuthStore.getState().signOut();
  }
  throw new ApiError(
    res.status,
    problem.code ?? `HTTP_${res.status}`,
    problem.detail ?? fallbackMessage(res.status),
    {
      errors: problem.errors,
      retryAfterSeconds: problem.retryAfterSeconds,
    },
  );
}

function fallbackMessage(status: number) {
  if (status === 502 || status === 503 || status === 504)
    return 'Máy chủ đang khởi động, thử lại sau ít phút.';
  if (status === 401) return 'Phiên đăng nhập đã hết hạn. Đăng nhập lại.';
  return 'Đã có lỗi xảy ra. Thử lại sau.';
}
