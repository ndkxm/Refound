/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    // Màu, font, khoảng cách của design system (việc V07) khai báo ở đây.
    extend: {},
  },
  plugins: [],
};
