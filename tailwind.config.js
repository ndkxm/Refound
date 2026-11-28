const { colors } = require('./src/components/ui/tokens');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      // Màu chữ/nền phụ dùng thang neutral mặc định của Tailwind (500 trên trắng, 400 trên 950).
      colors,
      fontSize: {
        display: ['28px', '34px'],
        title: ['20px', '28px'],
        body: ['16px', '24px'],
        label: ['14px', '20px'],
        caption: ['12px', '16px'],
      },
      // Vùng chạm tối thiểu 44dp (min-h-touch, min-w-touch).
      minHeight: { touch: '44px' },
      minWidth: { touch: '44px' },
    },
  },
  plugins: [],
};
