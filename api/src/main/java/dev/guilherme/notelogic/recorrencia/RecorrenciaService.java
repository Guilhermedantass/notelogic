package dev.guilherme.notelogic.recorrencia;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaResponse;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.recorrencia.RecorrenciaDtos.RecorrenciaRequest;
import dev.guilherme.notelogic.recorrencia.RecorrenciaDtos.RecorrenciaResponse;
import dev.guilherme.notelogic.tarefa.TarefaRepository;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class RecorrenciaService {

    private final RecorrenciaRepository repository;
    private final TarefaRepository tarefas;
    private final GeradorDeOcorrencias gerador;
    private final MateriaService materias;
    private final UsuarioAtual usuario;
    private final Clock clock;

    public RecorrenciaService(RecorrenciaRepository repository, TarefaRepository tarefas, GeradorDeOcorrencias gerador,
            MateriaService materias, UsuarioAtual usuario, Clock clock) {
        this.repository = repository;
        this.tarefas = tarefas;
        this.gerador = gerador;
        this.materias = materias;
        this.usuario = usuario;
        this.clock = clock;
    }

    @Transactional
    public Recorrencia criar(RecorrenciaRequest dados) {
        RegraRecorrencia regra = dados.regra().paraRegra(hoje());
        regra.validar();
        Recorrencia rec = repository.save(new Recorrencia(usuario.id(), dados.titulo(), dados.descricao(),
                materiaDe(dados.materiaId()), dados.prioridade(), regra, dados.acumulaOuPadrao()));
        gerador.sincronizar(usuario.id(), hoje()); // já cria a ocorrência de hoje (ou a próxima)
        return rec;
    }

    /**
     * "Esta e as próximas": a regra muda, as ocorrências pendentes a partir do dia são refeitas com o molde novo.
     * As anteriores e as já feitas ficam como estavam (são histórico).
     */
    @Transactional
    public Recorrencia editarAPartirDe(Long id, LocalDate aPartirDe, RecorrenciaRequest dados) {
        Recorrencia rec = buscar(id);
        RegraRecorrencia regra = dados.regra().paraRegra(rec.regra().inicio());
        regra.validar();
        rec.editar(dados.titulo(), dados.descricao(), materiaDe(dados.materiaId()), dados.prioridade(), regra,
                dados.acumulaOuPadrao());
        tarefas.apagarPendentesAPartirDe(id, aPartirDe);
        rec.marcarGeradaAte(aPartirDe.minusDays(1));
        gerador.sincronizar(usuario.id(), hoje());
        return rec;
    }

    /** "Parar de repetir a partir desta": nada mais é gerado, e as pendentes daqui para frente somem. */
    @Transactional
    public void encerrarAPartirDe(Long id, LocalDate aPartirDe) {
        Recorrencia rec = buscar(id);
        rec.encerrarEm(aPartirDe.minusDays(1));
        tarefas.apagarPendentesAPartirDe(id, aPartirDe);
    }

    /** Todas as regras, com a constância de cada uma ("fez 18 de 20"). */
    @Transactional(readOnly = true)
    public List<RecorrenciaResponse> listar() {
        LocalDate hoje = hoje();
        Map<Long, Long> feitas = tarefas.contarFeitasPorRecorrencia(usuario.id()).stream()
                .collect(Collectors.toMap(TarefaRepository.FeitasPorRecorrencia::getRecorrenciaId,
                        TarefaRepository.FeitasPorRecorrencia::getFeitas));
        return repository.findByUsuarioIdOrderByIdAsc(usuario.id()).stream()
                .map(r -> responder(r, feitas.getOrDefault(r.getId(), 0L), hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public Recorrencia buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recorrência", id));
    }

    private static RecorrenciaResponse responder(Recorrencia r, long feitas, LocalDate hoje) {
        RegraRecorrencia regra = r.regra();
        // esperadas: quantas vezes a regra caiu do início até hoje (ou até o fim, se já terminou)
        LocalDate ate = regra.fim() != null && regra.fim().isBefore(hoje) ? regra.fim() : hoje;
        long esperadas = ate.isBefore(regra.inicio()) ? 0 : CalendarioRecorrencia.entre(regra, regra.inicio(), ate).size();
        if (regra.maxOcorrencias() != null) {
            esperadas = Math.min(esperadas, regra.maxOcorrencias());
        }
        MateriaResponse materia = r.getMateria() != null ? MateriaResponse.de(r.getMateria()) : null;
        return new RecorrenciaResponse(r.getId(), r.getTitulo(), r.getDescricao(), materia, r.getPrioridade(), regra,
                CalendarioRecorrencia.descrever(regra), r.isAcumula(), r.encerrada(hoje), feitas, esperadas);
    }

    private Materia materiaDe(Long materiaId) {
        return materiaId != null ? materias.buscar(materiaId) : null;
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }
}
