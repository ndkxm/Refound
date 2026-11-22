import { vars } from 'nativewind';

/**
 * Nguồn màu duy nhất của app ("sổ tay giấy ấm"). Đây là chỗ DUY NHẤT được viết mã màu.
 * Giá trị dạng "R G B" để Tailwind ghép được độ trong suốt: bg-ink/10.
 * Trong component dùng className (bg-paper, text-ink…); chỗ không nhận className thì dùng useThemeColors().
 */
export const palette = {
  light: {
    paper: '246 243 236', // #F6F3EC nền
    surface: '255 253 248', // #FFFDF8 thẻ, ô nhập
    ink: '27 26 23', // #1B1A17 chữ chính
    muted: '107 103 94', // #6B675E chữ phụ, tương phản 5:1 trên nền paper
    line: '228 222 210', // #E4DED2 đường kẻ
    accent: '31 107 79', // #1F6B4F "còn tiêu được"
    warning: '183 121 31', // #B7791F sắp hết tiền (F21)
    danger: '179 65 46', // #B3412E vượt mức (F21)
  },
  dark: {
    paper: '22 21 19',
    surface: '31 29 26',
    ink: '241 236 226',
    muted: '156 151 140',
    line: '46 43 39',
    accent: '95 191 147',
    warning: '224 166 75',
    danger: '231 116 95',
  },
} as const;

export type ColorScheme = keyof typeof palette;
export type ColorToken = keyof (typeof palette)['light'];

export function toRgb(value: string): string {
  return `rgb(${value.split(' ').join(', ')})`;
}

function toVars(colors: Record<ColorToken, string>) {
  return vars(
    Object.fromEntries(Object.entries(colors).map(([token, value]) => [`--color-${token}`, value])),
  );
}

/** Đặt lên View gốc để mọi className bg-paper, text-ink… đổi theo chế độ sáng/tối. */
export const themeVars = { light: toVars(palette.light), dark: toVars(palette.dark) };
