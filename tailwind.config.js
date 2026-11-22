// Tên token khớp src/theme/colors.ts; giá trị thật nằm ở đó, đặt qua CSS variable nên tự đổi theo sáng/tối.
const tokens = [
  'paper',
  'surface',
  'ink',
  'muted',
  'line',
  'primary',
  'accent',
  'warning',
  'danger',
];

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  // Sáng/tối đi qua CSS variable (src/theme/colors.ts), không dùng class dark:. Để 'media' thì bản web
  // báo lỗi "Cannot manually set color scheme" mỗi khi class của thẻ <html> đổi.
  darkMode: 'class',
  theme: {
    // Font, cỡ chữ, khoảng cách của design system (việc V07) khai báo tiếp ở đây.
    extend: {
      colors: Object.fromEntries(
        tokens.map((token) => [token, `rgb(var(--color-${token}) / <alpha-value>)`]),
      ),
    },
  },
  plugins: [],
};
