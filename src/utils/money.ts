/**
 * Tiền luôn lưu dạng số nguyên VNĐ (bigint trong Postgres, number trong app).
 * Không dùng số thực cho tiền.
 */

/** 82000 → "82.000đ", -35000 → "-35.000đ" */
export function formatVnd(amount: number): string {
  if (!Number.isFinite(amount)) {
    throw new RangeError(`Số tiền không hợp lệ: ${amount}`);
  }
  const rounded = Math.round(amount);
  const sign = rounded < 0 ? '-' : '';
  const digits = Math.abs(rounded)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${digits}đ`;
}
