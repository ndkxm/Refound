import { useEffect, useState } from 'react';

import { api } from './api-client';

const BANNER_AFTER_MS = 3_000;
const WARMUP_TIMEOUT_MS = 90_000;

/**
 * Gọi `GET /health` khi mở app để đánh thức server Render (ngủ sau 15 phút, 07 §7.5).
 * `waking = true` khi quá 3 s chưa có phản hồi, tắt khi server trả lời hoặc hết 90 s.
 */
export function useServerWarmup() {
  const [waking, setWaking] = useState(false);

  useEffect(() => {
    const abort = new AbortController();
    const timer = setTimeout(() => setWaking(true), BANNER_AFTER_MS);
    api('/health', { timeoutMs: WARMUP_TIMEOUT_MS, signal: abort.signal })
      .catch(() => {
        // Lỗi thật sẽ hiện ở màn đang gọi API; banner chỉ báo đang chờ.
      })
      .finally(() => {
        clearTimeout(timer);
        if (!abort.signal.aborted) setWaking(false);
      });
    return () => {
      abort.abort();
      clearTimeout(timer);
    };
  }, []);

  return { waking };
}
