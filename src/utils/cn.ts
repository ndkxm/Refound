/** Ghép className, bỏ giá trị rỗng/false. Không gộp lớp xung đột: lớp truyền vào sau không đè được lớp trước. */
export const cn = (...classes: (string | false | null | undefined)[]) =>
  classes.filter(Boolean).join(' ');
