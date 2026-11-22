// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const prettierConfig = require('eslint-config-prettier');

module.exports = defineConfig([
  expoConfig,
  prettierConfig,
  {
    // Edge Functions chạy trên Deno, không lint bằng cấu hình React Native.
    ignores: ['dist/*', '.expo/*', 'supabase/functions/**'],
  },
]);
