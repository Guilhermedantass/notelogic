package dev.guilherme.notelogic.recorrencia;

public enum TipoRecorrencia {
    /** Todo dia. */
    DIARIA,
    /** Segunda a sexta. */
    DIAS_UTEIS,
    /** Nos dias da semana escolhidos (ex.: segunda e quarta). */
    SEMANAL,
    /** A cada N dias, contando a partir do início. */
    A_CADA_N_DIAS,
    /** Todo mês no dia X (dia 31 em fevereiro cai no último dia do mês). */
    MENSAL
}
