import { ApiError } from './api-error';

const MAX_RETRIES = 3;

/**
 * Chính sách thử lại của query (07 §7.5): tối đa 3 lần, chỉ lỗi mạng/hết giờ (`status 0`) và 502/503/504
 * (Render đang khởi động). Không thử lại 4xx, 500 hay lỗi code của app. Mutation không dùng hàm này: không tự gửi lại POST.
 */
export function isRetryable(failureCount: number, error: unknown): boolean {
  if (failureCount >= MAX_RETRIES || !(error instanceof ApiError)) return false;
  return error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504;
}

/** 2, 4, 8 giây. `attempt` là số lần đã lỗi trước đó (bắt đầu từ 0). */
export const retryDelay = (attempt: number) => 2000 * 2 ** attempt;
