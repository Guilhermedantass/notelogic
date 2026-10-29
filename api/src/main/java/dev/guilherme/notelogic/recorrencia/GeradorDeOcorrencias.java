package dev.guilherme.notelogic.recorrencia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaRepository;

/**
 * Cria as tarefas de cada dia a partir das regras, SOB DEMANDA: roda quando a lista de tarefas é pedida.
 * Assim não depende de nenhum job rodando à meia-noite, e quem não abre o app não acumula lixo no banco.
 */
@Service
public class GeradorDeOcorrencias {

    /**
     * DECISÃO DE PRODUTO (provisória): quantas ocorrências pendentes cada regra mostra de uma vez.
     * 1 = só a próxima. Para mostrar, por exemplo, a semana seguinte inteira, é só aumentar.
     */
    static final int OCORRENCIAS_PENDENTES = 1;

    /** Uma regra "acumula" esquecida por meses não gera centenas de atrasadas de uma vez. */
    private static final int LIMITE_ATRASADAS = 31;

    private final RecorrenciaRepository recorrencias;
    private final TarefaRepository tarefas;

    public GeradorDeOcorrencias(RecorrenciaRepository recorrencias, TarefaRepository tarefas) {
        this.recorrencias = recorrencias;
        this.tarefas = tarefas;
    }

    @Transactional
    public void sincronizar(Long usuarioId, LocalDate hoje) {
        for (Recorrencia regra : recorrencias.travarDoUsuario(usuarioId)) {
            sincronizar(regra, hoje);
        }
    }

    private void sincronizar(Recorrencia rec, LocalDate hoje) {
        RegraRecorrencia regra = rec.regra();

        // 1. "Expira": o que ficou para trás sem ser feito sai da lista
        if (!rec.isAcumula()) {
            tarefas.expirarPendentesAntesDe(rec.getId(), hoje);
        }

        // 2. As datas que já chegaram (até hoje) e ainda não viraram tarefa
        LocalDate base = rec.getUltimaGerada() != null ? rec.getUltimaGerada() : regra.inicio().minusDays(1);
        List<LocalDate> chegaram = new ArrayList<>();
        Optional<LocalDate> data = CalendarioRecorrencia.proxima(regra, base);
        while (data.isPresent() && !data.get().isAfter(hoje)) {
            chegaram.add(data.get());
            data = CalendarioRecorrencia.proxima(regra, data.get());
        }
        if (!chegaram.isEmpty()) {
            // "expira" só cria a de hoje (os dias perdidos já passaram); "acumula" cria as atrasadas também
            List<LocalDate> criar = rec.isAcumula()
                    ? chegaram.subList(Math.max(0, chegaram.size() - LIMITE_ATRASADAS), chegaram.size())
                    : chegaram.contains(hoje) ? List.of(hoje) : List.of();
            criar.forEach(dia -> criarOcorrencia(rec, dia));
            rec.marcarGeradaAte(chegaram.getLast());
        }

        // 3. Sempre deixar à vista a(s) próxima(s), mesmo que ainda não seja o dia
        long pendentes = tarefas.countByRecorrenciaIdAndConcluidaFalseAndExpiradaFalse(rec.getId());
        while (pendentes < OCORRENCIAS_PENDENTES) {
            LocalDate depoisDe = rec.getUltimaGerada() == null || rec.getUltimaGerada().isBefore(hoje)
                    ? hoje
                    : rec.getUltimaGerada();
            Optional<LocalDate> proxima = CalendarioRecorrencia.proxima(regra, depoisDe);
            if (proxima.isEmpty()) {
                break; // a regra terminou
            }
            criarOcorrencia(rec, proxima.get());
            rec.marcarGeradaAte(proxima.get());
            pendentes++;
        }
    }

    private void criarOcorrencia(Recorrencia rec, LocalDate dia) {
        // depois de um "esta e as próximas", o dia pode já ter uma ocorrência concluída: não duplica
        if (!tarefas.existsByRecorrenciaIdAndDataOcorrencia(rec.getId(), dia)) {
            tarefas.save(Tarefa.ocorrencia(rec, dia));
        }
    }
}
