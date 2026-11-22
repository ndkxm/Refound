/**
 * Nguồn màu duy nhất của app. Đây là chỗ DUY NHẤT được viết mã màu.
 * Giá trị dạng "R G B" để Tailwind ghép được độ trong suốt: bg-ink/10.
 * Trong component dùng className (bg-paper, text-ink…); chỗ không nhận className thì dùng useThemeColors().
 * Đổi màu ở đây thì sửa luôn biến CSS trong src/global.css (colors.test.ts báo nếu lệch).
 *
 * Xanh dương tin cậy cho hành động và điều hướng (primary), xanh lá cho tiền "còn tiêu được" (accent).
 * Chữ tương phản cao: ink ~18:1, muted ~7:1 trên nền; cả sáng lẫn tối đều đạt AA.
 * Chữ nằm trên nền primary dùng màu paper.
 */
export const palette = {
  light: {
    paper: '244 246 250', // #F4F6FA nền
    surface: '255 255 255', // #FFFFFF thẻ, thanh tab, ô nhập
    ink: '11 18 32', // #0B1220 chữ chính
    muted: '71 85 105', // #475569 chữ phụ
    line: '217 224 235', // #D9E0EB đường kẻ
    primary: '29 78 216', // #1D4ED8 nút Ghi, tab đang chọn, nút chính
    accent: '4 120 87', // #047857 "còn tiêu được", số dương
    warning: '180 83 9', // #B45309 sắp hết tiền (F21)
    danger: '185 28 28', // #B91C1C vượt mức (F21)
  },
  dark: {
    paper: '11 16 32',
    surface: '21 27 46',
    ink: '241 245 249',
    muted: '165 177 198',
    line: '38 48 73',
    primary: '138 176 255',
    accent: '74 222 154',
    warning: '251 191 36',
    danger: '255 142 134',
  },
} as const;

export type ColorScheme = keyof typeof palette;
export type ColorToken = keyof (typeof palette)['light'];

export function toRgb(value: string): string {
  return `rgb(${value.split(' ').join(', ')})`;
}
