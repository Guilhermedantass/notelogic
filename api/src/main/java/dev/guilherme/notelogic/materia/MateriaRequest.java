package dev.guilherme.notelogic.materia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MateriaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve ser uma cor no formato #RRGGBB") String cor) {
}
