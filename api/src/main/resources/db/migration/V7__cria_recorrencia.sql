-- A REGRA de repetição. As tarefas de cada dia (ocorrências) continuam na tabela tarefa.
create table recorrencia (
    id               bigint generated always as identity primary key,
    usuario_id       bigint                   not null references usuario (id) on delete cascade,
    titulo           varchar(200)             not null,
    descricao        varchar(2000),
    materia_id       bigint references materia (id) on delete set null,
    prioridade       varchar(10)              not null default 'MEDIA',
    tipo             varchar(15)              not null,
    dias_semana      varchar(60),             -- "MONDAY,WEDNESDAY" (ver DiasDaSemanaConverter)
    intervalo_dias   integer,
    dia_do_mes       integer,
    inicio           date                     not null,
    fim              date,
    max_ocorrencias  integer,
    acumula          boolean                  not null default false,
    ultima_gerada    date,                    -- até que dia as ocorrências já foram criadas
    criada_em        timestamp with time zone not null
);

create index recorrencia_usuario_id on recorrencia (usuario_id);

-- Cada ocorrência é uma tarefa comum que sabe de qual regra e de qual dia ela é.
-- Apagar a regra não apaga o histórico: as tarefas ficam, só perdem o vínculo.
alter table tarefa
    add column recorrencia_id   bigint references recorrencia (id) on delete set null,
    add column data_ocorrencia  date,
    add column expirada         boolean not null default false;

-- Nunca duas ocorrências da mesma regra no mesmo dia (o banco garante, mesmo com requisições simultâneas)
create unique index tarefa_ocorrencia_unica on tarefa (recorrencia_id, data_ocorrencia);
