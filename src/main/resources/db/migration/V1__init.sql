-- Splitly initial schema.
-- Flyway runs this once and records it in flyway_schema_history. Migrations are
-- append-only: to change something later, add V2__..., never edit this file.

create table users (
    id            bigserial     primary key,
    name          varchar(100)  not null,
    email         varchar(255)  not null,
    password_hash varchar(255),
    created_at    timestamptz   not null default now(),
    updated_at    timestamptz   not null default now(),
    constraint uk_users_email unique (email)
);

-- Named expense_groups, not "groups": GROUPS is a keyword in Postgres and would
-- need quoting everywhere. The Java entity is still called Group.
create table expense_groups (
    id         bigserial    primary key,
    name       varchar(100) not null,
    currency   char(3)      not null default 'INR',
    created_by bigint       not null references users (id),
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create table group_members (
    id        bigserial   primary key,
    group_id  bigint      not null references expense_groups (id) on delete cascade,
    user_id   bigint      not null references users (id),
    role      varchar(20) not null default 'MEMBER',
    joined_at timestamptz not null default now(),
    -- A user can only be in a group once. Enforced here, not just in Java:
    -- two concurrent requests can both pass an application-level check.
    constraint uk_group_members unique (group_id, user_id)
);

create table expenses (
    id           bigserial     primary key,
    group_id     bigint        not null references expense_groups (id) on delete cascade,
    description  varchar(255)  not null,
    amount       numeric(19, 2) not null,
    paid_by      bigint        not null references users (id),
    split_type   varchar(20)   not null,
    expense_date date          not null,
    created_by   bigint        not null references users (id),
    created_at   timestamptz   not null default now(),
    updated_at   timestamptz   not null default now(),
    -- Soft delete: balances stay auditable and nothing cascades away silently.
    deleted_at   timestamptz,
    constraint ck_expenses_amount_positive check (amount > 0)
);

-- One row per participant: how much THIS user owes for THIS expense.
-- The invariant sum(share_amount) = expenses.amount is enforced in the service
-- layer, because SQL cannot express a cross-row check cheaply.
create table expense_shares (
    id           bigserial      primary key,
    expense_id   bigint         not null references expenses (id) on delete cascade,
    user_id      bigint         not null references users (id),
    share_amount numeric(19, 2) not null,
    constraint uk_expense_shares unique (expense_id, user_id),
    constraint ck_expense_shares_non_negative check (share_amount >= 0)
);

-- An actual cash transfer between two members, kept separate from expenses.
create table settlements (
    id         bigserial      primary key,
    group_id   bigint         not null references expense_groups (id) on delete cascade,
    from_user  bigint         not null references users (id),
    to_user    bigint         not null references users (id),
    amount     numeric(19, 2) not null,
    note       varchar(255),
    settled_at timestamptz    not null default now(),
    constraint ck_settlements_amount_positive check (amount > 0),
    constraint ck_settlements_distinct_users check (from_user <> to_user)
);

create index idx_group_members_user on group_members (user_id);
create index idx_expenses_group on expenses (group_id);
create index idx_expense_shares_expense on expense_shares (expense_id);
create index idx_expense_shares_user on expense_shares (user_id);
create index idx_settlements_group on settlements (group_id);
