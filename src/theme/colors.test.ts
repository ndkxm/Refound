import { palette, toRgb } from './colors';

describe('palette', () => {
  it('chế độ tối có đủ đúng các token của chế độ sáng', () => {
    expect(Object.keys(palette.dark).sort()).toEqual(Object.keys(palette.light).sort());
  });

  it.each(['light', 'dark'] as const)('mọi màu %s đều là "R G B" hợp lệ', (scheme) => {
    for (const value of Object.values(palette[scheme])) {
      const parts = value.split(' ').map(Number);
      expect(parts).toHaveLength(3);
      parts.forEach((n) => expect(n >= 0 && n <= 255 && Number.isInteger(n)).toBe(true));
    }
  });
});

describe('toRgb', () => {
  it('đổi "R G B" sang chuỗi rgb() cho chỗ không dùng className', () => {
    expect(toRgb('27 26 23')).toBe('rgb(27, 26, 23)');
  });
});
