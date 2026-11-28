import { profileSchema } from './profile-schema';

const parse = (displayName: string) => profileSchema.safeParse({ displayName });

describe('profileSchema', () => {
  it('nhận tên 2 và 40 ký tự', () => {
    expect(parse('An').success).toBe(true);
    expect(parse('a'.repeat(40)).success).toBe(true);
  });

  it('từ chối tên 1 và 41 ký tự', () => {
    expect(parse('A').success).toBe(false);
    expect(parse('a'.repeat(41)).success).toBe(false);
  });

  it('bỏ khoảng trắng hai đầu trước khi đếm', () => {
    const r = parse('  Nguyễn Văn A  ');
    expect(r.success && r.data.displayName).toBe('Nguyễn Văn A');
    expect(parse('  A  ').success).toBe(false);
    expect(parse('     ').success).toBe(false);
  });

  it('lỗi có câu tiếng Việt', () => {
    const r = parse('A');
    expect(r.success ? null : r.error.issues[0].message).toBe('Tên hiển thị dài 2–40 ký tự');
  });
});
