package dev.guilherme.notelogic.materia;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;

@Service
public class MateriaService {

    private final MateriaRepository repository;

    public MateriaService(MateriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Materia> listar() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Materia buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Matéria", id));
    }

    @Transactional
    public Materia criar(String nome, String cor) {
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe uma matéria chamada " + nome);
        }
        return repository.save(new Materia(nome, cor));
    }

    @Transactional
    public Materia editar(Long id, String nome, String cor) {
        Materia materia = buscar(id);
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflitoException("Já existe uma matéria chamada " + nome);
        }
        materia.editar(nome, cor);
        return materia;
    }

    /** As tarefas da matéria não são apagadas: o banco (on delete set null) as deixa sem matéria. */
    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }
}
