/** Lỗi từ server (ProblemDetail, 06 §6.1) hoặc lỗi mạng (`status = 0`). `message` là câu tiếng Việt hiện được cho người dùng. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  /** Lỗi từng trường khi `code = VALIDATION_FAILED`. */
  readonly errors: { field: string; message: string }[];
  readonly retryAfterSeconds?: number;

  constructor(
    status: number,
    code: string,
    message: string,
    extra: { errors?: { field: string; message: string }[]; retryAfterSeconds?: number } = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.errors = extra.errors ?? [];
    this.retryAfterSeconds = extra.retryAfterSeconds;
  }
}

export const NETWORK_ERROR = 'NETWORK_ERROR';
export const TIMEOUT = 'TIMEOUT';
