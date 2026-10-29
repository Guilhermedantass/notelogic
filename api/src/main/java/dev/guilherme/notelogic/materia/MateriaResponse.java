package dev.guilherme.notelogic.materia;

public record MateriaResponse(Long id, String nome, String cor) {

    public static MateriaResponse de(Materia materia) {
        return new MateriaResponse(materia.getId(), materia.getNome(), materia.getCor());
    }
}
