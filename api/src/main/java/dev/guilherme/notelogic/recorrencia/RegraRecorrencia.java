package dev.guilherme.notelogic.recorrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import dev.guilherme.notelogic.erro.RegraVioladaException;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * "Quando algo se repete": é o que o CalendarioRecorrencia entende. Usada pelas tarefas recorrentes e pelos eventos
 * da agenda. É um @Embeddable: as colunas ficam na tabela de quem a usa (recorrencia, evento), sem tabela própria.
 * Como é um record, o Hibernate monta pelo construtor, e "mudar a regra" é sempre trocar por outra.
 *
 * @param diasSemana     usado só no SEMANAL
 * @param intervaloDias  usado só no A_CADA_N_DIAS
 * @param diaDoMes       usado só no MENSAL
 * @param fim            último dia possível (inclusive), ou null para nunca terminar
 * @param maxOcorrencias termina depois de tantas vezes, ou null
 */
@Embeddable
public record RegraRecorrencia(
        @Enumerated(EnumType.STRING) TipoRecorrencia tipo,
        @Convert(converter = DiasDaSemanaConverter.class) Set<DayOfWeek> diasSemana,
        Integer intervaloDias,
        Integer diaDoMes,
        LocalDate inicio,
        LocalDate fim,
        Integer maxOcorrencias) {

    public RegraRecorrencia {
        diasSemana = diasSemana == null ? Set.of() : Set.copyOf(diasSemana);
    }

    /** A mesma regra, parando de valer depois de {@code ultimoDia}. */
    public RegraRecorrencia comFim(LocalDate ultimoDia) {
        return new RegraRecorrencia(tipo, diasSemana, intervaloDias, diaDoMes, inicio, ultimoDia, maxOcorrencias);
    }

    /**
     * Coerência entre os campos (o formato de cada um o DTO já validou).
     * Não fica no construtor porque uma regra ENCERRADA pode ter fim antes do início.
     */
    public void validar() {
        switch (tipo) {
            case SEMANAL -> exigir(!diasSemana.isEmpty(), "Escolha pelo menos um dia da semana");
            case A_CADA_N_DIAS -> exigir(intervaloDias != null, "Informe a cada quantos dias");
            case MENSAL -> exigir(diaDoMes != null, "Informe o dia do mês");
            default -> {
            }
        }
        exigir(inicio != null, "Informe o início");
        exigir(fim == null || !fim.isBefore(inicio), "O fim não pode ser antes do início");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraVioladaException(mensagem);
        }
    }
}
