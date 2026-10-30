-- Eventos que se repetem ("treinar toda segunda 9h"). A regra é a mesma das tarefas recorrentes (RegraRecorrencia),
-- mas as ocorrências NÃO são salvas: são calculadas quando a agenda pede um período, porque não há nada para concluir.
alter table evento
    add column repeticao_tipo            varchar(15),
    add column repeticao_dias_semana     varchar(60),
    add column repeticao_intervalo_dias  integer,
    add column repeticao_dia_do_mes      integer,
    add column repeticao_inicio          date,   -- sempre igual a data_inicio
    add column repeticao_fim             date,   -- último dia possível (fim do semestre), ou nulo para sempre
    add column repeticao_max_ocorrencias integer,

    -- Só eventos de um dia se repetem, e a regra começa no próprio evento
    add constraint evento_repeticao_de_um_dia
        check (repeticao_tipo is null or (data_fim = data_inicio and repeticao_inicio = data_inicio));

-- "Só esta": a ocorrência de um dia foi cancelada ou mudou de data, horário ou local.
-- Guarda a ocorrência inteira como ficou (e não só o que mudou), para a regra ser simples de ler.
create table evento_excecao (
    id           bigint generated always as identity primary key,
    evento_id    bigint                   not null references evento (id) on delete cascade,
    data         date                     not null,  -- o dia ORIGINAL da ocorrência, pela regra
    cancelada    boolean                  not null,
    nova_data    date,
    hora_inicio  time,
    hora_fim     time,
    local        varchar(200),
    criada_em    timestamp with time zone not null,

    constraint evento_excecao_cancelada_ou_com_data check (cancelada or nova_data is not null)
);

create unique index evento_excecao_unica on evento_excecao (evento_id, data);
create index evento_excecao_nova_data on evento_excecao (evento_id, nova_data);
