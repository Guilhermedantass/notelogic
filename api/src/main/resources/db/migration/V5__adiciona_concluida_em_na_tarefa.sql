alter table tarefa add column concluida_em timestamp with time zone;

-- Tarefas concluídas antes desta versão não têm a data real de conclusão.
-- A melhor aproximação disponível é a data de criação: melhor do que deixar nulo e sumir das estatísticas.
update tarefa set concluida_em = criada_em where concluida;

create index tarefa_concluida_em on tarefa (concluida_em);
