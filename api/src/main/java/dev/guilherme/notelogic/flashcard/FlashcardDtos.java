package dev.guilherme.notelogic.flashcard;

import java.time.LocalDate;

import dev.guilherme.notelogic.materia.MateriaResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Os DTOs de entrada e saída do módulo, agrupados por serem pequenos e só usados aqui. */
public final class FlashcardDtos {

    private FlashcardDtos() {
    }

    public record BaralhoRequest(
            @NotBlank @Size(max = 100) String nome,
            @Size(max = 500) String descricao,
            Long materiaId) {
    }

    public record BaralhoResponse(Long id, String nome, String descricao, MateriaResponse materia,
            long totalCartoes, long paraRevisarHoje) {

        static BaralhoResponse de(BaralhoService.Resumo resumo) {
            Baralho b = resumo.baralho();
            MateriaResponse materia = b.getMateria() != null ? MateriaResponse.de(b.getMateria()) : null;
            return new BaralhoResponse(b.getId(), b.getNome(), b.getDescricao(), materia, resumo.totalCartoes(),
                    resumo.paraRevisarHoje());
        }
    }

    public record CartaoRequest(
            @NotBlank @Size(max = 1000) String frente,
            @NotBlank @Size(max = 2000) String verso) {
    }

    public record CartaoResponse(Long id, String frente, String verso, LocalDate proximaRevisao,
            int intervaloDias, int repeticoes, double facilidade) {

        static CartaoResponse de(Cartao c) {
            EstadoAgendamento estado = c.estado();
            return new CartaoResponse(c.getId(), c.getFrente(), c.getVerso(), c.getProximaRevisao(),
                    estado.intervaloDias(), estado.repeticoes(), estado.facilidade());
        }
    }

    public record RevisaoRequest(@NotNull Resposta resposta) {
    }
}
