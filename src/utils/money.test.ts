import { formatVnd } from './money';

describe('formatVnd', () => {
  it.each([
    [0, '0đ'],
    [500, '500đ'],
    [35000, '35.000đ'],
    [82000, '82.000đ'],
    [1000000, '1.000.000đ'],
    [-35000, '-35.000đ'],
  ])('%d → %s', (amount, expected) => {
    expect(formatVnd(amount)).toBe(expected);
  });

  it('làm tròn số lẻ về đồng gần nhất', () => {
    expect(formatVnd(35000.6)).toBe('35.001đ');
  });

  it('báo lỗi khi không phải số hữu hạn', () => {
    expect(() => formatVnd(Number.NaN)).toThrow(RangeError);
  });
});
