package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.agenda.AgendaDtos.EventoRequest;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.erro.RegraVioladaException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class EventoService {

    /** Mais que isso ninguém vê de uma vez: o mês na agenda tem no máximo 6 semanas, e o Hoje olha 30 dias. */
    static final int MAX_DIAS_POR_CONSULTA = 366;

    private final EventoRepository repository;
    private final CategoriaEventoService categorias;
    private final MateriaService materias;
    private final UsuarioAtual usuario;

    public EventoService(EventoRepository repository, CategoriaEventoService categorias, MateriaService materias,
            UsuarioAtual usuario) {
        this.repository = repository;
        this.categorias = categorias;
        this.materias = materias;
        this.usuario = usuario;
    }

    @Transactional(readOnly = true)
    public List<Evento> noPeriodo(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraVioladaException("O fim do período não pode ser antes do início");
        }
        if (ChronoUnit.DAYS.between(de, ate) >= MAX_DIAS_POR_CONSULTA) {
            throw new RegraVioladaException("Consulte no máximo " + MAX_DIAS_POR_CONSULTA + " dias por vez");
        }
        return repository.noPeriodo(usuario.id(), de, ate);
    }

    @Transactional(readOnly = true)
    public Evento buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", id));
    }

    @Transactional
    public Evento criar(EventoRequest dados) {
        return repository.save(new Evento(usuario.id(), dados.titulo(), dados.descricao(), dados.local(),
                categoriaDe(dados.categoriaId()), materiaDe(dados.materiaId()), dados.periodo()));
    }

    @Transactional
    public Evento editar(Long id, EventoRequest dados) {
        Evento evento = buscar(id);
        evento.editar(dados.titulo(), dados.descricao(), dados.local(), categoriaDe(dados.categoriaId()),
                materiaDe(dados.materiaId()), dados.periodo());
        return evento;
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscar(id));
    }

    private CategoriaEvento categoriaDe(Long categoriaId) {
        return categoriaId != null ? categorias.buscar(categoriaId) : null;
    }

    private Materia materiaDe(Long materiaId) {
        return materiaId != null ? materias.buscar(materiaId) : null;
    }
}
