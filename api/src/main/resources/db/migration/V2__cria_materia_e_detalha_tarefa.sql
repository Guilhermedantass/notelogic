create table materia (
    id         bigint generated always as identity primary key,
    nome       varchar(100)             not null,
    cor        varchar(7)               not null,
    criada_em  timestamp with time zone not null
);

create unique index materia_nome_unico on materia (lower(nome));

alter table tarefa
    add column materia_id bigint references materia (id) on delete set null,
    add column prazo      date,
    add column prioridade varchar(10) not null default 'MEDIA';

create index tarefa_materia_id on tarefa (materia_id);
