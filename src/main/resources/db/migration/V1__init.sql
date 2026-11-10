create table app_user
(
    id            bigint generated always as identity primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name  varchar(100) not null,
    created_at    timestamptz  not null
);

create table expense_group
(
    id         bigint generated always as identity primary key,
    name       varchar(100) not null,
    owner_id   bigint       not null references app_user (id),
    created_at timestamptz  not null
);

-- Участник группы. Может быть привязан к зарегистрированному пользователю,
-- а может быть просто именем (друг, которого нет в приложении).
create table group_member
(
    id       bigint generated always as identity primary key,
    group_id bigint       not null references expense_group (id) on delete cascade,
    user_id  bigint references app_user (id),
    name     varchar(100) not null,
    unique (group_id, user_id),
    unique (group_id, name)
);

-- Ссылки на участников (payer_id, member_id, from/to_member_id) проверяются при коммите
-- (deferrable initially deferred). Иначе при удалении группы одним запросом PostgreSQL
-- проверил бы их раньше, чем каскадно удалит траты и переводы.

-- Суммы хранятся в копейках.
create table expense
(
    id          bigint generated always as identity primary key,
    group_id    bigint       not null references expense_group (id) on delete cascade,
    payer_id    bigint       not null references group_member (id) deferrable initially deferred,
    description varchar(200) not null,
    amount      bigint       not null check (amount > 0),
    split_type  varchar(20)  not null,
    spent_on    date         not null,
    created_by  bigint       not null references app_user (id),
    created_at  timestamptz  not null
);

create table expense_share
(
    expense_id bigint not null references expense (id) on delete cascade,
    member_id  bigint not null references group_member (id) deferrable initially deferred,
    amount     bigint not null check (amount >= 0),
    primary key (expense_id, member_id)
);

create table settlement
(
    id             bigint generated always as identity primary key,
    group_id       bigint      not null references expense_group (id) on delete cascade,
    from_member_id bigint      not null references group_member (id) deferrable initially deferred,
    to_member_id   bigint      not null references group_member (id) deferrable initially deferred,
    amount         bigint      not null check (amount > 0),
    created_by     bigint      not null references app_user (id),
    created_at     timestamptz not null,
    check (from_member_id <> to_member_id)
);

create index idx_group_member_user on group_member (user_id);
create index idx_expense_group on expense (group_id);
create index idx_expense_share_member on expense_share (member_id);
create index idx_settlement_group on settlement (group_id);
