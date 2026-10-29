create table baralho (
    id          bigint generated always as identity primary key,
    nome        varchar(100)             not null,
    descricao   varchar(500),
    materia_id  bigint references materia (id) on delete set null,
    criado_em   timestamp with time zone not null
);

create index baralho_materia_id on baralho (materia_id);

-- Os campos repeticoes, intervalo_dias, facilidade e proxima_revisao são o estado do algoritmo SM-2
create table cartao (
    id               bigint generated always as identity primary key,
    baralho_id       bigint                   not null references baralho (id) on delete cascade,
    frente           varchar(1000)            not null,
    verso            varchar(2000)            not null,
    repeticoes       integer                  not null default 0,
    intervalo_dias   integer                  not null default 0,
    facilidade       double precision         not null default 2.5,
    proxima_revisao  date                     not null,
    criado_em        timestamp with time zone not null
);

-- A consulta mais frequente é "cartões deste baralho que vencem até hoje"
create index cartao_baralho_revisao on cartao (baralho_id, proxima_revisao);

-- Histórico de respostas: alimenta as estatísticas
create table revisao (
    id              bigint generated always as identity primary key,
    cartao_id       bigint                   not null references cartao (id) on delete cascade,
    resposta        varchar(10)              not null,
    intervalo_dias  integer                  not null,
    respondida_em   timestamp with time zone not null
);

create index revisao_respondida_em on revisao (respondida_em);
