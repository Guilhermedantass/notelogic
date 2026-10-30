-- Agenda: eventos são o que ACONTECE num dia/horário (prova, aula, treino). O que se CONCLUI continua sendo tarefa.

-- Categorias são da pessoa (nome e cor livres). "destaque" faz os eventos dela aparecerem em "datas importantes".
create table categoria_evento (
    id          bigint generated always as identity primary key,
    usuario_id  bigint                   not null references usuario (id) on delete cascade,
    nome        varchar(50)              not null,
    cor         varchar(7)               not null,
    destaque    boolean                  not null default false,
    criada_em   timestamp with time zone not null
);

create unique index categoria_evento_nome_unico on categoria_evento (usuario_id, lower(nome));

-- As categorias prontas para quem já tem conta (as contas novas ganham as mesmas em CategoriasPadrao)
insert into categoria_evento (usuario_id, nome, cor, destaque, criada_em)
select u.id, c.nome, c.cor, c.destaque, now()
from usuario u
cross join (values ('Prova', '#E5484D', true),
                   ('Aula', '#3B82F6', false),
                   ('Treino', '#22A06B', false),
                   ('Compromisso', '#F59E0B', false)) as c (nome, cor, destaque);

-- Data e hora sem fuso: "treinar 9h" é 9h no relógio, em qualquer época do ano.
create table evento (
    id            bigint generated always as identity primary key,
    usuario_id    bigint                   not null references usuario (id) on delete cascade,
    titulo        varchar(200)             not null,
    descricao     varchar(2000),
    local         varchar(200),
    categoria_id  bigint references categoria_evento (id) on delete set null,
    materia_id    bigint references materia (id) on delete set null,
    data_inicio   date                     not null,
    data_fim      date                     not null,  -- igual a data_inicio num evento de um dia
    dia_inteiro   boolean                  not null,
    hora_inicio   time,
    hora_fim      time,                               -- opcional: "prova às 14h" pode não ter fim
    criado_em     timestamp with time zone not null,

    -- As mesmas regras do PeriodoEvento, aqui como última linha de defesa
    constraint evento_fim_depois_do_inicio check (data_fim >= data_inicio),
    constraint evento_dia_inteiro_sem_hora check (dia_inteiro = (hora_inicio is null)),
    constraint evento_dia_inteiro_sem_fim  check (not dia_inteiro or hora_fim is null),
    constraint evento_hora_fim_depois      check (hora_fim is null or data_fim > data_inicio or hora_fim > hora_inicio)
);

create index evento_usuario_periodo on evento (usuario_id, data_inicio);
