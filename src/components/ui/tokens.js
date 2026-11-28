// Bảng màu của design system. Một nguồn cho cả tailwind.config.js (className)
// và component cần giá trị màu thật (ActivityIndicator, placeholderTextColor).
// CommonJS vì tailwind.config.js đọc bằng require.
// Cặp màu chữ/nền đều đạt tương phản WCAG AA (≥ 4,5:1) ở cả chế độ sáng và tối.

const colors = {
  // emerald: 700 trên nền trắng 5,5:1; 400 trên nền 950 tối 9:1
  brand: {
    50: '#ecfdf5',
    100: '#d1fae5',
    400: '#34d399',
    600: '#059669',
    700: '#047857',
    900: '#064e3b',
    950: '#022c22',
  },
  // red: 700 trên nền trắng 6,5:1; 400 trên nền tối 7:1
  danger: {
    50: '#fef2f2',
    400: '#f87171',
    700: '#b91c1c',
    950: '#450a0a',
  },
  // amber cho banner cảnh báo (máy chủ đang khởi động)
  warn: {
    100: '#fef3c7',
    800: '#92400e',
    900: '#78350f',
    950: '#451a03',
  },
};

// Màu dùng ngoài className, theo chế độ sáng/tối.
const native = {
  // onSolid: chữ/vòng quay trên nút nền đậm (primary, danger)
  light: { spinner: colors.brand[700], placeholder: '#737373', onSolid: '#ffffff' },
  dark: { spinner: colors.brand[400], placeholder: '#a3a3a3', onSolid: colors.brand[950] },
};

module.exports = { colors, native };
