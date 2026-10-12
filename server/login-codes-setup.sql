-- =====================================================================
-- CANTINA — tabella dei codici di accesso via email
-- Da eseguire UNA VOLTA in Supabase > SQL Editor. Non contiene dati da modificare.
-- Si può eseguire in anticipo: finché LOGIN_MODE non è "code" la tabella resta vuota.
-- =====================================================================
create table if not exists public.login_codes (
  id         bigserial primary key,
  email      text        not null,
  purpose    text        not null check (purpose in ('login', 'link')),
  code_hash  text        not null,            -- impronta del codice: il codice vero non viene mai salvato
  expires_at timestamptz not null,
  attempts   integer     not null default 0,
  used       boolean     not null default false,
  ip         text,
  created_at timestamptz not null default now()
);

create index if not exists login_codes_email_idx on public.login_codes (email, purpose, created_at desc);
create index if not exists login_codes_ip_idx    on public.login_codes (ip, created_at desc);

-- Accessibile solo dalla funzione (service role): nessuna policy.
alter table public.login_codes enable row level security;
