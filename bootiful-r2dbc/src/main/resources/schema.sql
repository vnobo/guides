drop table if exists users;
create table users
(
    id       serial primary key,
    username varchar(64)  not null unique,
    password varchar(512) not null,
    enabled  boolean      not null default true
);
