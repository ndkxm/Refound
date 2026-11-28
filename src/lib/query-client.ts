import { QueryClient } from '@tanstack/react-query';

import { isRetryable, retryDelay } from './is-retryable';

// Persist cache (NFR-08) thuộc P1-16.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: isRetryable, retryDelay },
    // Không tự gửi lại POST/PATCH (tránh tạo hai tin); màn hiện lỗi + nút "Thử lại" (07 §7.5).
    mutations: { retry: false },
  },
});
