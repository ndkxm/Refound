import { z } from 'zod';

// Khớp UpdateMeRequest ở server: bỏ khoảng trắng hai đầu rồi dài 2–40 (đếm như String.length của Java).
export const profileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'Tên hiển thị dài 2–40 ký tự')
    .max(40, 'Tên hiển thị dài 2–40 ký tự'),
});

export type ProfileForm = z.infer<typeof profileSchema>;
