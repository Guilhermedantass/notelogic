package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.agenda.AgendaDtos.EventoRequest;
import dev.guilherme.notelogic.agenda.AgendaDtos.OcorrenciaRequest;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;
import dev.guilherme.notelogic.erro.RegraVioladaException;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaService;
import dev.guilherme.notelogic.recorrencia.CalendarioRecorrencia;
import dev.guilherme.notelogic.recorrencia.RegraRecorrencia;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class EventoService {

    /** Mais que isso ninguém vê de uma vez: o mês na agenda tem no máximo 6 semanas, e o Hoje olha 30 dias. */
    static final int MAX_DIAS_POR_CONSULTA = 366;

    /** Na agenda, no mesmo dia: dia inteiro primeiro, depois por horário. */
    private static final Comparator<OcorrenciaEvento> ORDEM_NA_AGENDA = Comparator
            .comparing((OcorrenciaEvento o) -> o.periodo().dataInicio())
            .thenComparing(o -> !o.periodo().diaInteiro())
            .thenComparing(o -> o.periodo().horaInicio(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(o -> o.evento().getId());

    private final EventoRepository repository;
    private final EventoExcecaoRepository excecoes;
    private final CategoriaEventoService categorias;
    private final MateriaService materias;
    private final UsuarioAtual usuario;

    public EventoService(EventoRepository repository, EventoExcecaoRepository excecoes,
            CategoriaEventoService categorias, MateriaService materias, UsuarioAtual usuario) {
        this.repository = repository;
        this.excecoes = excecoes;
        this.categorias = categorias;
        this.materias = materias;
        this.usuario = usuario;
    }

    /** Tudo o que acontece em [de, ate]: os avulsos e as ocorrências calculadas dos que se repetem. */
    @Transactional(readOnly = true)
    public List<OcorrenciaEvento> noPeriodo(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraVioladaException("O fim do período não pode ser antes do início");
        }
        if (ChronoUnit.DAYS.between(de, ate) >= MAX_DIAS_POR_CONSULTA) {
            throw new RegraVioladaException("Consulte no máximo " + MAX_DIAS_POR_CONSULTA + " dias por vez");
        }
        List<OcorrenciaEvento> resultado = new ArrayList<>();
        repository.avulsosNoPeriodo(usuario.id(), de, ate).forEach(e -> resultado.add(OcorrenciaEvento.avulso(e)));

        List<Evento> recorrentes = repository.recorrentesAte(usuario.id(), ate);
        if (!recorrentes.isEmpty()) {
            Map<Long, Map<LocalDate, EventoExcecao>> excecoesPorEvento = excecoes
                    .doPeriodo(recorrentes.stream().map(Evento::getId).toList(), de, ate).stream()
                    .collect(Collectors.groupingBy(EventoExcecao::getEventoId,
                            Collectors.toMap(EventoExcecao::getData, x -> x)));
            for (Evento evento : recorrentes) {
                resultado.addAll(ocorrencias(evento, excecoesPorEvento.getOrDefault(evento.getId(), Map.of()), de, ate));
            }
        }
        resultado.sort(ORDEM_NA_AGENDA);
        return resultado;
    }

    /**
     * As datas da regra no período, mais as que foram movidas para dentro dele vindas de fora.
     * Cada uma passa pela exceção do dia, se houver: cancelada some, alterada muda de data/horário/local.
     */
    private static List<OcorrenciaEvento> ocorrencias(Evento evento, Map<LocalDate, EventoExcecao> excecoesDoEvento,
            LocalDate de, LocalDate ate) {
        RegraRecorrencia regra = evento.getRepeticao();
        TreeSet<LocalDate> dias = new TreeSet<>(CalendarioRecorrencia.ocorrencias(regra, de, ate));
        excecoesDoEvento.keySet().stream().filter(dia -> ehOcorrencia(regra, dia)).forEach(dias::add);

        PeriodoEvento padrao = evento.periodo();
        List<OcorrenciaEvento> lista = new ArrayList<>();
        for (LocalDate dia : dias) {
            EventoExcecao excecao = excecoesDoEvento.get(dia);
            OcorrenciaEvento ocorrencia;
            if (excecao == null) {
                ocorrencia = new OcorrenciaEvento(evento, dia,
                        new PeriodoEvento(dia, dia, padrao.diaInteiro(), padrao.horaInicio(), padrao.horaFim()),
                        evento.getLocal(), false);
            } else if (excecao.isCancelada()) {
                continue;
            } else {
                ocorrencia = new OcorrenciaEvento(evento, dia, new PeriodoEvento(excecao.getNovaData(),
                        excecao.getNovaData(), padrao.diaInteiro(), excecao.getHoraInicio(), excecao.getHoraFim()),
                        excecao.getLocal(), true);
            }
            LocalDate quando = ocorrencia.periodo().dataInicio();
            if (!quando.isBefore(de) && !quando.isAfter(ate)) {
                lista.add(ocorrencia);
            }
        }
        return lista;
    }

    private static boolean ehOcorrencia(RegraRecorrencia regra, LocalDate dia) {
        return !CalendarioRecorrencia.ocorrencias(regra, dia, dia).isEmpty();
    }

    @Transactional(readOnly = true)
    public Evento buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", id));
    }

    @Transactional
    public Evento criar(EventoRequest dados) {
        PeriodoEvento periodo = dados.periodo();
        return repository.save(new Evento(usuario.id(), dados.titulo(), dados.descricao(), dados.local(),
                categoriaDe(dados.categoriaId()), materiaDe(dados.materiaId()), periodo, repeticaoDe(dados, periodo)));
    }

    /**
     * Sem {@code aPartirDe} (ou a partir do primeiro dia), edita o evento inteiro.
     * Num que se repete, "esta e as próximas" encerra a série na véspera e cria outra a partir dali com os dados
     * novos (como o Google Agenda): o que já passou fica como era. Devolve o evento que ficou com os dados novos.
     */
    @Transactional
    public Evento editar(Long id, LocalDate aPartirDe, EventoRequest dados) {
        Evento evento = buscar(id);
        if (!dividir(evento, aPartirDe)) {
            PeriodoEvento periodo = dados.periodo();
            RegraRecorrencia repeticao = repeticaoDe(dados, periodo);
            evento.editar(dados.titulo(), dados.descricao(), dados.local(), categoriaDe(dados.categoriaId()),
                    materiaDe(dados.materiaId()), periodo, repeticao);
            if (repeticao == null) {
                excecoes.apagarDoEvento(id); // deixou de se repetir: as exceções não valem mais nada
            }
            return evento;
        }
        RegraRecorrencia antes = evento.getRepeticao();
        int jaAconteceram = CalendarioRecorrencia.ocorrencias(antes, antes.inicio(), aPartirDe.minusDays(1)).size();
        encerrar(evento, aPartirDe);

        PeriodoEvento periodo = dados.periodo();
        RegraRecorrencia repeticao = repeticaoDe(dados, periodo);
        if (repeticao != null && repeticao.maxOcorrencias() != null && antes.maxOcorrencias() != null) {
            // "10 vezes" continua valendo para as duas partes juntas: a nova fica com as que faltam
            repeticao = new RegraRecorrencia(repeticao.tipo(), repeticao.diasSemana(), repeticao.intervaloDias(),
                    repeticao.diaDoMes(), repeticao.inicio(), repeticao.fim(),
                    Math.max(1, repeticao.maxOcorrencias() - jaAconteceram));
        }
        return repository.save(new Evento(usuario.id(), dados.titulo(), dados.descricao(), dados.local(),
                categoriaDe(dados.categoriaId()), materiaDe(dados.materiaId()), periodo, repeticao));
    }

    /** Sem {@code aPartirDe} (ou a partir do primeiro dia), apaga o evento. Num que se repete, "esta e as próximas". */
    @Transactional
    public void remover(Long id, LocalDate aPartirDe) {
        Evento evento = buscar(id);
        if (dividir(evento, aPartirDe)) {
            encerrar(evento, aPartirDe);
        } else {
            repository.delete(evento);
        }
    }

    /** "Só esta": muda a data, o horário ou o local de uma ocorrência. */
    @Transactional
    public void alterarOcorrencia(Long id, LocalDate dia, OcorrenciaRequest dados) {
        Evento evento = buscarOcorrencia(id, dia);
        PeriodoEvento periodo = PeriodoEvento.de(dados.data(), null, evento.periodo().diaInteiro(),
                dados.horaInicio(), dados.horaFim());
        EventoExcecao excecao = excecaoDe(evento, dia);
        excecao.alterar(periodo, dados.local());
        excecoes.save(excecao);
    }

    /** "Só esta": a ocorrência do dia não acontece (não tem aula hoje). */
    @Transactional
    public void cancelarOcorrencia(Long id, LocalDate dia) {
        EventoExcecao excecao = excecaoDe(buscarOcorrencia(id, dia), dia);
        excecao.cancelar();
        excecoes.save(excecao);
    }

    private Evento buscarOcorrencia(Long id, LocalDate dia) {
        Evento evento = buscar(id);
        if (!evento.seRepete()) {
            throw new RegraVioladaException("Este evento não se repete: edite o evento inteiro");
        }
        if (!ehOcorrencia(evento.getRepeticao(), dia)) {
            throw new RegraVioladaException("Este evento não acontece em " + dia);
        }
        return evento;
    }

    /** A exceção que já existe para o dia, ou uma nova ainda NÃO salva: só é salva depois de preenchida (check da V9). */
    private EventoExcecao excecaoDe(Evento evento, LocalDate dia) {
        return excecoes.findByEventoIdAndData(evento.getId(), dia)
                .orElseGet(() -> new EventoExcecao(evento.getId(), dia));
    }

    /** "A partir de" só divide a série quando o evento se repete e o dia vem depois do primeiro. */
    private static boolean dividir(Evento evento, LocalDate aPartirDe) {
        return aPartirDe != null && evento.seRepete() && aPartirDe.isAfter(evento.periodo().dataInicio());
    }

    /** A série para na véspera; as exceções dali para frente pertenciam à parte que acabou. */
    private void encerrar(Evento evento, LocalDate aPartirDe) {
        evento.encerrarRepeticaoEm(aPartirDe.minusDays(1));
        excecoes.apagarAPartirDe(evento.getId(), aPartirDe);
    }

    /** A regra começa sempre no próprio evento, e só eventos de um dia se repetem. */
    private static RegraRecorrencia repeticaoDe(EventoRequest dados, PeriodoEvento periodo) {
        if (dados.repeticao() == null) {
            return null;
        }
        if (!periodo.dataFim().equals(periodo.dataInicio())) {
            throw new RegraVioladaException("Só eventos de um dia podem se repetir");
        }
        RegraRecorrencia regra = dados.repeticao().comecandoEm(periodo.dataInicio());
        regra.validar();
        return regra;
    }

    private CategoriaEvento categoriaDe(Long categoriaId) {
        return categoriaId != null ? categorias.buscar(categoriaId) : null;
    }

    private Materia materiaDe(Long materiaId) {
        return materiaId != null ? materias.buscar(materiaId) : null;
    }
}
