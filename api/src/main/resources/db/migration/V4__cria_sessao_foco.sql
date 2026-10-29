create table sessao_foco (
    id                     bigint generated always as identity primary key,
    tipo                   varchar(15)              not null,
    status                 varchar(15)              not null,
    duracao_planejada_min  integer                  not null,
    iniciada_em            timestamp with time zone not null,
    finalizada_em          timestamp with time zone,
    segundos_focados       integer,
    tarefa_id              bigint references tarefa (id) on delete set null,
    materia_id             bigint references materia (id) on delete set null
);

create index sessao_foco_iniciada_em on sessao_foco (iniciada_em);

-- Índice único PARCIAL: só vale para as linhas EM_ANDAMENTO. Como a expressão é constante,
-- no máximo uma linha pode estar em andamento. É a garantia final, mesmo com duas requisições simultâneas.
create unique index sessao_foco_uma_em_andamento on sessao_foco ((true)) where status = 'EM_ANDAMENTO';
