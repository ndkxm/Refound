// Kiểu dữ liệu từ server, khớp DTO Java (06 §6.2). Viết tay cho tới khi sinh được từ OpenAPI (ghi chú S06).

/** `MeDto` (auth/application/MeDto.java). */
export type Me = {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  role: 'user' | 'admin';
  reputation: number;
  authProvider: string;
  notificationPrefs: { match: boolean; claim: boolean; chat: boolean; expiry: boolean };
};

/** `LoginResult` của `POST /auth/firebase`. */
export type LoginResult = { token: string; user: Me; isNew: boolean };

/** `GET /health`. */
export type Health = { status: 'up'; db?: 'up' | 'down' };
