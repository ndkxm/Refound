import { View, type ColorValue } from 'react-native';
import Svg, { Circle, Path, Rect } from 'react-native-svg';

// Một bộ icon nét (stroke) trên lưới 24x24, cùng độ dày nét và bo tròn đầu nét. Cùng bộ trên Android, iOS, web
// nên không còn lệch giữa các nền tảng như SF Symbols / Material Symbols. Thêm icon: thêm một dòng vào đây.
type Shape =
  | { d: string }
  | { circle: [cx: number, cy: number, r: number] }
  | { rect: [x: number, y: number, w: number, h: number, rx: number] };

const d = (path: string): Shape => ({ d: path });

const shapes = {
  home: [
    d('M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8'),
    d(
      'M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
    ),
  ],
  book: [
    d('M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20'),
  ],
  plus: [d('M5 12h14'), d('M12 5v14')],
  pin: [
    d(
      'M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0',
    ),
    { circle: [12, 10, 3] },
  ],
  users: [
    d('M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2'),
    { circle: [9, 7, 4] },
    d('M22 21v-2a4 4 0 0 0-3-3.87'),
    d('M16 3.13a4 4 0 0 1 0 7.75'),
  ],
  user: [d('M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2'), { circle: [12, 7, 4] }],
  mic: [
    d('M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z'),
    d('M19 10v2a7 7 0 0 1-14 0v-2'),
    d('M12 19v3'),
  ],
  keyboard: [
    { rect: [2, 4, 20, 16, 2] },
    d('M6 8h.01'),
    d('M10 8h.01'),
    d('M14 8h.01'),
    d('M18 8h.01'),
    d('M6 12h.01'),
    d('M10 12h.01'),
    d('M14 12h.01'),
    d('M18 12h.01'),
    d('M7 16h10'),
  ],
  camera: [
    d('M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z'),
    { circle: [12, 13, 3] },
  ],
  pencil: [d('M12 20h9'), d('M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z')],
  wallet: [
    d(
      'M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1',
    ),
    d('M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4'),
  ],
  x: [d('M18 6 6 18'), d('m6 6 12 12')],
} satisfies Record<string, Shape[]>;

export type IconName = keyof typeof shapes;

type Props = {
  name: IconName;
  color: ColorValue;
  size?: number;
  /** Tab đang chọn vẽ nét đậm hơn một chút. */
  strokeWidth?: number;
};

/** Icon trang trí: luôn đi kèm chữ hoặc có accessibilityLabel ở nút bọc ngoài, nên ẩn khỏi trình đọc màn hình. */
export function Icon({ name, color, size = 24, strokeWidth = 1.75 }: Props) {
  const common = { stroke: color, strokeWidth, fill: 'none' } as const;
  return (
    <View
      style={{ width: size, height: size }}
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      <Svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {shapes[name].map((shape: Shape, i) => {
          if ('d' in shape) return <Path key={i} d={shape.d} {...common} />;
          if ('circle' in shape) {
            const [cx, cy, r] = shape.circle;
            return <Circle key={i} cx={cx} cy={cy} r={r} {...common} />;
          }
          const [x, y, width, height, rx] = shape.rect;
          return <Rect key={i} x={x} y={y} width={width} height={height} rx={rx} {...common} />;
        })}
      </Svg>
    </View>
  );
}
