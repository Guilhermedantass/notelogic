package dev.guilherme.notelogic.agenda;

import java.time.LocalDate;
import java.time.LocalTime;

import dev.guilherme.notelogic.materia.MateriaResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AgendaDtos {

    private AgendaDtos() {
    }

    /** Quando o evento não tem categoria nem matéria, ele usa a cor do app. */
    static final String COR_PADRAO = "#543EFE";

    public record CategoriaRequest(
            @NotBlank @Size(max = 50) String nome,
            @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve ser uma cor no formato #RRGGBB") String cor,
            Boolean destaque) {

        /** Opcional no JSON: sem o campo, a categoria não é destacada. */
        boolean destaqueOuPadrao() {
            return Boolean.TRUE.equals(destaque);
        }
    }

    public record CategoriaResponse(Long id, String nome, String cor, boolean destaque) {

        public static CategoriaResponse de(CategoriaEvento c) {
            return new CategoriaResponse(c.getId(), c.getNome(), c.getCor(), c.isDestaque());
        }
    }

    /** Formato de cada campo. A coerência entre datas e horas fica em PeriodoEvento. */
    public record EventoRequest(
            @NotBlank @Size(max = 200) String titulo,
            @Size(max = 2000) String descricao,
            @Size(max = 200) String local,
            Long categoriaId,
            Long materiaId,
            @NotNull LocalDate dataInicio,
            LocalDate dataFim,
            Boolean diaInteiro,
            LocalTime horaInicio,
            LocalTime horaFim) {

        PeriodoEvento periodo() {
            return PeriodoEvento.de(dataInicio, dataFim, Boolean.TRUE.equals(diaInteiro), horaInicio, horaFim);
        }
    }

    /** {@code cor} já vem resolvida: a da categoria, senão a da matéria, senão a do app. */
    public record EventoResponse(Long id, String titulo, String descricao, String local, CategoriaResponse categoria,
            MateriaResponse materia, LocalDate dataInicio, LocalDate dataFim, boolean diaInteiro,
            LocalTime horaInicio, LocalTime horaFim, String cor) {

        public static EventoResponse de(Evento e) {
            PeriodoEvento p = e.periodo();
            CategoriaResponse categoria = e.getCategoria() != null ? CategoriaResponse.de(e.getCategoria()) : null;
            MateriaResponse materia = e.getMateria() != null ? MateriaResponse.de(e.getMateria()) : null;
            String cor = categoria != null ? categoria.cor() : materia != null ? materia.cor() : COR_PADRAO;
            return new EventoResponse(e.getId(), e.getTitulo(), e.getDescricao(), e.getLocal(), categoria, materia,
                    p.dataInicio(), p.dataFim(), p.diaInteiro(), p.horaInicio(), p.horaFim(), cor);
        }
    }
}
