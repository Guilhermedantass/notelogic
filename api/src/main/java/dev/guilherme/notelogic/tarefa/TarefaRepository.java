package dev.guilherme.notelogic.tarefa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findAllByOrderByIdAsc();
}
