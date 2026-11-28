-- P1-01: bảng P1. Thiết kế: docs/kien-truc/02-du-lieu.md 2.2, máy trạng thái 05 §5.2.
-- Bảng con chỉ sống theo cha (match_candidates, handovers, chat_messages, push_tokens) thì ON DELETE CASCADE;
-- claims, reports, reputation_events là lịch sử nên giữ mặc định (chặn xóa cha).

-- Ứng viên khớp giữa tin mất và tin nhặt (04 §4.4). Đối chiếu nền upsert theo cặp.
create table refound.match_candidates (
  id            uuid primary key default gen_random_uuid(),
  lost_item_id  uuid not null references refound.items(id) on delete cascade,
  found_item_id uuid not null references refound.items(id) on delete cascade,
  score         real not null check (score between 0 and 1),
  components    jsonb not null,  -- {v, d, t, c}
  source        text not null default 'auto' check (source in ('auto','finder_report')),
  status        text not null default 'active' check (status in ('active','dismissed')),
  notified_at   timestamptz,
  created_at    timestamptz not null default now(),
  unique (lost_item_id, found_item_id),
  check (lost_item_id <> found_item_id)
);
create index match_candidates_found_ix on refound.match_candidates (found_item_id);

-- Yêu cầu nhận đồ (05 §5.2). finder_id sao chép từ tin để kiểm quyền không cần join.
create table refound.claims (
  id            uuid primary key default gen_random_uuid(),
  found_item_id uuid not null references refound.items(id),
  lost_item_id  uuid references refound.items(id),
  claimant_id   uuid not null references refound.users(id),
  finder_id     uuid not null references refound.users(id),
  status        text not null default 'submitted'
                check (status in ('submitted','accepted','rejected','cancelled','completed','disputed')),
  answer        text not null check (char_length(answer) between 1 and 200),
  reject_reason text check (char_length(reject_reason) between 1 and 200),
  cancel_reason text check (char_length(cancel_reason) between 1 and 200),
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  constraint claims_not_own_item_ck check (claimant_id <> finder_id),                         -- C1, X9
  constraint claims_rejected_reason_ck check (status <> 'rejected' or reject_reason is not null) -- C3, C4b
);
-- Mỗi người chỉ có một claim đang mở trên một tin (C1).
create unique index claims_one_open_per_claimant_ux on refound.claims (found_item_id, claimant_id)
  where status in ('submitted','accepted');
-- Mỗi tin chỉ có một claim được chấp nhận: hai luồng cùng accept thì một luồng vỡ unique (C2).
create unique index claims_one_accepted_per_item_ux on refound.claims (found_item_id)
  where status = 'accepted';
create index claims_item_claimant_ix on refound.claims (found_item_id, claimant_id);  -- đếm lần bị từ chối (BR-05)
create index claims_claimant_ix on refound.claims (claimant_id, updated_at desc);
create index claims_finder_ix   on refound.claims (finder_id, updated_at desc);
create index claims_open_ix     on refound.claims (updated_at) where status in ('submitted','accepted');  -- C9

-- Bàn giao (05 C5b–C6b). Dòng tạo khi accept; mã sinh lại thì ghi đè (mã cũ mất hiệu lực).
create table refound.handovers (
  claim_id        uuid primary key references refound.claims(id) on delete cascade,
  code_hash       text check (code_hash ~ '^[0-9a-f]{64}$'),  -- sha256 hex, không lưu mã gốc
  code_expires_at timestamptz,
  method          text check (method in ('qr','manual')),
  note            text check (char_length(note) between 10 and 200),
  completed_at    timestamptz,
  constraint handovers_code_ck check ((code_hash is null) = (code_expires_at is null)),
  constraint handovers_done_ck check ((method is null) = (completed_at is null)),
  constraint handovers_manual_note_ck check (method is distinct from 'manual' or note is not null)  -- C6b
);

create table refound.chat_messages (
  id         bigint generated always as identity primary key,
  claim_id   uuid not null references refound.claims(id) on delete cascade,
  sender_id  uuid not null references refound.users(id),
  body       text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);
create index chat_messages_claim_ix on refound.chat_messages (claim_id, id);  -- phân trang after=id

-- Tiêu đề, nội dung thông báo dựng từ type + payload lúc đọc (không lưu chữ hiển thị).
create table refound.notifications (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references refound.users(id) on delete cascade,
  type       text not null,
  payload    jsonb not null default '{}'::jsonb,
  read_at    timestamptz,
  created_at timestamptz not null default now()
);
create index notifications_user_ix   on refound.notifications (user_id, created_at desc);
create index notifications_unread_ix on refound.notifications (user_id) where read_at is null;  -- unreadCount

-- Một máy đổi người dùng thì token chuyển chủ (upsert theo token).
create table refound.push_tokens (
  token      text primary key,
  user_id    uuid not null references refound.users(id) on delete cascade,
  updated_at timestamptz not null default now()
);
create index push_tokens_user_ix on refound.push_tokens (user_id);

-- Báo cáo vi phạm theo tin (FR-ITEM-06) hoặc theo claim (tranh chấp C7).
create table refound.reports (
  id          uuid primary key default gen_random_uuid(),
  item_id     uuid references refound.items(id),
  claim_id    uuid references refound.claims(id),
  reporter_id uuid not null references refound.users(id),
  reason      text not null check (char_length(reason) between 1 and 500),
  status      text not null default 'open' check (status in ('open','valid','invalid')),
  created_at  timestamptz not null default now(),
  constraint reports_target_ck check (num_nonnulls(item_id, claim_id) = 1),
  unique (item_id, reporter_id),   -- một người báo một tin một lần (3 người khác nhau → under_review)
  unique (claim_id, reporter_id)
);
create index reports_open_ix on refound.reports (created_at) where status = 'open';  -- màn admin

-- users.reputation là tổng cache của bảng này, cập nhật cùng transaction.
create table refound.reputation_events (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references refound.users(id),
  delta      integer not null check (delta <> 0),
  reason     text not null,
  claim_id   uuid references refound.claims(id),
  created_at timestamptz not null default now()
);
create index reputation_events_user_ix on refound.reputation_events (user_id);
