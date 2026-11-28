import { ApiError, NETWORK_ERROR, TIMEOUT } from './api-error';
import { isRetryable, retryDelay } from './is-retryable';

const http = (status: number) => new ApiError(status, 'X', 'lỗi');

describe('isRetryable (07 §7.5)', () => {
  it.each([502, 503, 504])('thử lại khi server trả %i (Render đang thức dậy)', (status) => {
    expect(isRetryable(0, http(status))).toBe(true);
  });

  it('thử lại khi lỗi mạng hoặc hết thời gian chờ', () => {
    expect(isRetryable(0, new ApiError(0, NETWORK_ERROR, 'mất mạng'))).toBe(true);
    expect(isRetryable(0, new ApiError(0, TIMEOUT, 'quá lâu'))).toBe(true);
  });

  it.each([400, 401, 403, 404, 409, 413, 429, 500])('không thử lại khi server trả %i', (status) => {
    expect(isRetryable(0, http(status))).toBe(false);
  });

  it('không thử lại lỗi không phải ApiError (lỗi code của app)', () => {
    expect(isRetryable(0, new TypeError('x is undefined'))).toBe(false);
  });

  it('thử lại tối đa 3 lần: lần lỗi thứ 1, 2, 3 có thử lại, lần thứ 4 thì dừng', () => {
    const e = http(503);
    // TanStack gọi với số lần đã lỗi trước đó: 0, 1, 2, 3
    expect([0, 1, 2, 3].map((n) => isRetryable(n, e))).toEqual([true, true, true, false]);
  });
});

describe('retryDelay', () => {
  it('lùi 2, 4, 8 giây', () => {
    expect([0, 1, 2].map(retryDelay)).toEqual([2000, 4000, 8000]);
  });
});
