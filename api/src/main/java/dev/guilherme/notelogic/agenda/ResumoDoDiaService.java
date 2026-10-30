package dev.guilherme.notelogic.agenda;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.agenda.AgendaDtos.EventoResponse;
import dev.guilherme.notelogic.agenda.ResumoDoDia.DataImportante;
import dev.guilherme.notelogic.agenda.ResumoDoDia.Tipo;
import dev.guilherme.notelogic.tarefa.Prioridade;
import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaRepository;
import dev.guilherme.notelogic.usuario.UsuarioAtual;

@Service
public class ResumoDoDiaService {

    /** DECISÃO DE PRODUTO: "próximas datas importantes" olha até 30 dias à frente. */
    static final int DIAS_A_FRENTE = 30;

    private static final Comparator<DataImportante> ORDEM = Comparator.comparing(DataImportante::data)
            .thenComparing(DataImportante::hora, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(DataImportante::titulo);

    private final EventoService eventos;
    private final TarefaRepository tarefas;
    private final UsuarioAtual usuario;
    private final Clock clock;

    public ResumoDoDiaService(EventoService eventos, TarefaRepository tarefas, UsuarioAtual usuario, Clock clock) {
        this.eventos = eventos;
        this.tarefas = tarefas;
        this.usuario = usuario;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResumoDoDia resumo() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ate = hoje.plusDays(DIAS_A_FRENTE);

        List<EventoResponse> agenda = eventos.noPeriodo(hoje, hoje).stream().map(EventoResponse::de).toList();

        Stream<DataImportante> provas = eventos.noPeriodo(hoje, ate).stream()
                .filter(o -> o.evento().getCategoria() != null && o.evento().getCategoria().isDestaque())
                .map(o -> deEvento(o, hoje));
        Stream<DataImportante> entregas = tarefas
                .pendentesComPrazoEntre(usuario.id(), Prioridade.ALTA, hoje, ate).stream()
                .map(t -> deTarefa(t, hoje));

        return new ResumoDoDia(hoje, agenda, Stream.concat(provas, entregas).sorted(ORDEM).toList());
    }

    private static DataImportante deEvento(OcorrenciaEvento o, LocalDate hoje) {
        EventoResponse e = EventoResponse.de(o);
        return new DataImportante(Tipo.EVENTO, e.id(), e.titulo(), e.categoria().nome(), e.dataInicio(),
                e.horaInicio(), e.cor(), diasAte(e.dataInicio(), hoje));
    }

    private static DataImportante deTarefa(Tarefa t, LocalDate hoje) {
        String cor = t.getMateria() != null ? t.getMateria().getCor() : AgendaDtos.COR_PADRAO;
        return new DataImportante(Tipo.TAREFA, t.getId(), t.getTitulo(), "Tarefa", t.getPrazo(), null, cor,
                diasAte(t.getPrazo(), hoje));
    }

    /** Um evento de vários dias que já começou conta como hoje. */
    private static long diasAte(LocalDate data, LocalDate hoje) {
        return Math.max(0, ChronoUnit.DAYS.between(hoje, data));
    }
}
