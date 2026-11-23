-- P0-01: bảng lõi P0. Thiết kế: docs/kien-truc/02-du-lieu.md 2.2, 2.4.
-- Extension: Supabase đặt ở schema extensions
create extension if not exists vector  with schema extensions;
create extension if not exists postgis with schema extensions;

create schema if not exists refound;
set search_path = refound, extensions;

create table categories (
  code       text primary key,
  name_vi    text not null,
  group_code text not null,
  sensitive  boolean not null default false
);

insert into categories (code, name_vi, group_code, sensitive) values
  ('student_card', 'Thẻ sinh viên',      'documents',   true),
  ('id_card',      'CCCD / hộ chiếu',    'documents',   true),
  ('bank_card',    'Thẻ ngân hàng',      'documents',   true),
  ('wallet',       'Ví',                  'wallet_bag',  false),
  ('bag',          'Balo, túi',           'wallet_bag',  false),
  ('keys',         'Chìa khóa',           'keys',        false),
  ('phone',        'Điện thoại',          'electronics', false),
  ('earphones',    'Tai nghe',            'electronics', false),
  ('laptop',       'Laptop, máy tính bảng','electronics', false),
  ('charger',      'Sạc, cáp',            'electronics', false),
  ('clothing',     'Áo, mũ',              'clothing',    false),
  ('bottle',       'Bình nước',           'other',       false),
  ('book',         'Sách, vở',            'other',       false),
  ('other',        'Khác',                'other',       false);

create table users (
  id                 uuid primary key default gen_random_uuid(),
  firebase_uid       text not null unique,  -- 'sub' trong Firebase ID token
  display_name       text not null check (char_length(display_name) between 2 and 40),
  avatar_path        text,
  role               text not null default 'user'   check (role in ('user','admin')),
  status             text not null default 'active' check (status in ('active','locked','deleted')),
  reputation         integer not null default 0,
  notification_prefs jsonb not null default '{}'::jsonb,
  created_at         timestamptz not null default now()
);

create table items (
  id                 uuid primary key default gen_random_uuid(),
  author_id          uuid not null references users(id),
  type               text not null check (type in ('lost','found')),
  status             text not null default 'open'
                     check (status in ('open','claimed','returned','closed','expired','under_review')),
  category           text not null references categories(code),
  description        text not null check (char_length(description) between 5 and 1000),
  ai_caption         text,  -- mô tả ảnh do Groq sinh (ADR-02), không trả về app
  happened_at        timestamptz not null,
  location           geography(Point, 4326) not null,
  public_location    geography(Point, 4326) not null,
  radius_m           integer,
  embedding_status   text not null default 'pending'
                     check (embedding_status in ('pending','done','failed')),
  embedding_attempts smallint not null default 0,
  next_embed_at      timestamptz not null default now(),
  last_activity_at   timestamptz not null default now(),
  expiry_warned_at   timestamptz,
  closed_reason      text,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now(),
  deleted_at         timestamptz,
  constraint items_radius_ck check (
    (type = 'lost'  and radius_m is not null and radius_m between 50 and 2000) or
    (type = 'found' and radius_m is null)),
  constraint items_claimed_found_ck check (status <> 'claimed' or type = 'found'),          -- 5.1 I3
  constraint items_closed_reason_ck check (status <> 'closed' or closed_reason is not null)  -- 5.1 I7, I12
);
create index items_location_gix  on items using gist (location);
create index items_listing_ix    on items (type, status, happened_at desc) where deleted_at is null;
create index items_embed_todo_ix on items (next_embed_at) where embedding_status = 'pending';
create index items_author_ix     on items (author_id, created_at desc);

create table item_images (
  id         uuid primary key default gen_random_uuid(),
  item_id    uuid not null references items(id) on delete cascade,
  position   smallint not null check (position between 0 and 3),
  full_path  text not null,
  thumb_path text not null,
  created_at timestamptz not null default now(),
  unique (item_id, position)
);

create table item_embeddings (
  id         bigint generated always as identity primary key,
  item_id    uuid not null references items(id) on delete cascade,
  source     text not null check (source in ('text','image')),
  image_id   uuid references item_images(id) on delete cascade,
  model      text not null,
  embedding  vector(768) not null,
  created_at timestamptz not null default now(),
  check ((source = 'image') = (image_id is not null)),
  -- job chạy lại không tạo vector trùng; nulls not distinct để chữ (image_id null) cũng chỉ có một
  unique nulls not distinct (item_id, source, image_id)
);
create index item_embeddings_item_ix on item_embeddings (item_id);
-- Không tạo HNSW: vài trăm tin thì quét tuần tự cho kết quả chính xác và vẫn < 50 ms (ADR-03).

create table embedding_cache (
  key        text primary key,
  embedding  vector(768) not null,
  created_at timestamptz not null default now()
);

create table verification_secrets (
  item_id  uuid primary key references items(id) on delete cascade,
  question text not null check (char_length(question) between 5 and 200),
  answer   text not null check (char_length(answer)   between 1 and 200)
);
