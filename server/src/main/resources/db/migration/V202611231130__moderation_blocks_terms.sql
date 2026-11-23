-- R03, R04 (D14): chặn người dùng và đồng ý điều khoản. Thiết kế: docs/kien-truc/03-phan-quyen.md 3.5.

-- R04: phiên bản điều khoản đã đồng ý; null = chưa đồng ý lần nào.
alter table refound.users
  add column terms_version     smallint,
  add column terms_accepted_at timestamptz,
  add constraint users_terms_ck check ((terms_version is null) = (terms_accepted_at is null));

-- R03: A chặn B. Hiệu lực hai chiều (BR-15) nên truy vấn tra cả hai hướng.
create table refound.user_blocks (
  blocker_id uuid not null references refound.users(id) on delete cascade,
  blocked_id uuid not null references refound.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  constraint user_blocks_not_self_ck check (blocker_id <> blocked_id)
);
create index user_blocks_blocked_ix on refound.user_blocks (blocked_id, blocker_id);
