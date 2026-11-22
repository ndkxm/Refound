import { useColorScheme } from 'react-native';

import { palette, toRgb, type ColorScheme, type ColorToken } from './colors';

export function useColorSchemeName(): ColorScheme {
  return useColorScheme() === 'dark' ? 'dark' : 'light';
}

/** Màu dạng rgb() cho chỗ không nhận className: thanh tab, icon, theme của navigation. */
export function useThemeColors(): Record<ColorToken, string> {
  const colors = palette[useColorSchemeName()];
  return Object.fromEntries(
    Object.entries(colors).map(([token, value]) => [token, toRgb(value)]),
  ) as Record<ColorToken, string>;
}
