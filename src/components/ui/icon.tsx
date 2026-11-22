import { SymbolView, type SymbolViewProps } from 'expo-symbols';
import type { ColorValue } from 'react-native';

export type IconName = Extract<SymbolViewProps['name'], { android?: unknown }>;

type Props = {
  /** Tên Material Symbols cho Android, SF Symbols cho iOS. */
  name: Required<Pick<IconName, 'android' | 'ios'>>;
  color: ColorValue;
  size?: number;
};

export function Icon({ name, color, size = 24 }: Props) {
  // Web cũng dùng Material Symbols, nên lấy luôn tên của Android.
  return <SymbolView name={{ ...name, web: name.android }} tintColor={color} size={size} />;
}
