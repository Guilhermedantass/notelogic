package dev.guilherme.notelogic.recorrencia;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * AttributeConverter: ensina o JPA a guardar um tipo que o banco não conhece.
 * Set<DayOfWeek> {MONDAY, WEDNESDAY} ↔ coluna texto "MONDAY,WEDNESDAY".
 * (A alternativa seria uma tabela só para os dias, com @ElementCollection: exagero para no máximo 7 valores.)
 */
@Converter
public class DiasDaSemanaConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> dias) {
        if (dias == null || dias.isEmpty()) {
            return null;
        }
        return dias.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String coluna) {
        if (coluna == null || coluna.isBlank()) {
            return null; // o RegraRecorrencia troca por um conjunto vazio; nulo deixa o evento sem repetição ser nulo
        }
        return Arrays.stream(coluna.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
