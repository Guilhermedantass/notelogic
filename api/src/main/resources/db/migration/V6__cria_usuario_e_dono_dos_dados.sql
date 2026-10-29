create table usuario (
    id          bigint generated always as identity primary key,
    nome        varchar(100)             not null,
    email       varchar(254)             not null,
    senha_hash  varchar(100)             not null,
    criado_em   timestamp with time zone not null
);

create unique index usuario_email_unico on usuario (lower(email));

-- Cada dado passa a ter um dono. A coluna nasce nula porque os dados que já existem ainda não têm dono:
-- o primeiro usuário cadastrado herda todos eles (ver AdocaoDeDadosAntigos).
-- Cartões e revisões não precisam da coluna: pertencem ao dono do baralho.
alter table materia     add column usuario_id bigint references usuario (id) on delete cascade;
alter table tarefa      add column usuario_id bigint references usuario (id) on delete cascade;
alter table baralho     add column usuario_id bigint references usuario (id) on delete cascade;
alter table sessao_foco add column usuario_id bigint references usuario (id) on delete cascade;

create index materia_usuario_id     on materia (usuario_id);
create index tarefa_usuario_id      on tarefa (usuario_id);
create index baralho_usuario_id     on baralho (usuario_id);
create index sessao_foco_usuario_id on sessao_foco (usuario_id, iniciada_em);

-- Nome de matéria passa a ser único POR USUÁRIO: duas pessoas podem ter "Cálculo"
drop index materia_nome_unico;
create unique index materia_nome_unico on materia (usuario_id, lower(nome));

-- Uma sessão em andamento por vez, agora POR USUÁRIO
drop index sessao_foco_uma_em_andamento;
create unique index sessao_foco_uma_em_andamento on sessao_foco (usuario_id) where status = 'EM_ANDAMENTO';
