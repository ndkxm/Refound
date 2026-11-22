// Tên token khớp src/theme/colors.ts; giá trị thật nằm ở đó, đặt qua CSS variable nên tự đổi theo sáng/tối.
const tokens = ['paper', 'surface', 'ink', 'muted', 'line', 'accent', 'warning', 'danger'];

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
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
