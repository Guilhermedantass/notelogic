create table tarefa (
    id         bigint generated always as identity primary key,
    titulo     varchar(200)             not null,
    descricao  varchar(2000),
    concluida  boolean                  not null default false,
    criada_em  timestamp with time zone not null
);
