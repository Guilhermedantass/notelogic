package dev.guilherme.notelogic.materia;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class MateriaService {

    private final MateriaRepository repository;
    private final UsuarioAtual usuario;

    public MateriaService(MateriaRepository repository, UsuarioAtual usuario) {
        this.repository = repository;
        this.usuario = usuario;
    }

    @Transactional(readOnly = true)
    public List<Materia> listar() {
        return repository.findAllByUsuarioIdOrderByNomeAsc(usuario.id());
    }

    /**
     * A matéria de OUTRO usuário dá o mesmo 404 de uma que não existe.
     * Responder 403 ("existe, mas não é sua") já vazaria a informação de que aquele id existe.
     */
    @Transactional(readOnly = true)
    public Materia buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria", id));
    }

    @Transactional
    public Materia criar(String nome, String cor) {
        if (repository.existsByUsuarioIdAndNomeIgnoreCase(usuario.id(), nome)) {
            throw new ConflitoException("Já existe uma matéria chamada " + nome);
        }
        return repository.save(new Materia(usuario.id(), nome, cor));
    }

    @Transactional
    public Materia editar(Long id, String nome, String cor) {
        Materia materia = buscar(id);
        if (repository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(usuario.id(), nome, id)) {
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
