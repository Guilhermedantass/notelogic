package dev.guilherme.notelogic.materia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MateriaControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    MateriaRepository repository;

    @Autowired
    TarefaRepository tarefas;

    @BeforeEach
    void limpa() {
        tarefas.deleteAll();
        repository.deleteAll();
    }

    @Test
    void criaEListaEmOrdemAlfabetica() throws Exception {
        criar("Inglês", "#22AA55");
        criar("Grafos", "#543EFE");

        mvc.perform(get("/api/materias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Grafos"))
                .andExpect(jsonPath("$[1].nome").value("Inglês"));
    }

    @Test
    void nomeRepetidoDa409MesmoComOutraCaixa() throws Exception {
        criar("Grafos", "#543EFE");

        mvc.perform(post("/api/materias").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome": "GRAFOS", "cor": "#000000"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }

    @Test
    void corForaDoFormatoDa400() throws Exception {
        mvc.perform(post("/api/materias").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome": "Grafos", "cor": "roxo"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cor").value("deve ser uma cor no formato #RRGGBB"));
    }

    @Test
    void editaMantendoOProprioNome() throws Exception {
        Long id = repository.save(new Materia("Grafos", "#543EFE")).getId();

        mvc.perform(put("/api/materias/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome": "Grafos", "cor": "#111111"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cor").value("#111111"));
    }

    @Test
    void removerMateriaNaoApagaAsTarefas() throws Exception {
        Materia materia = repository.save(new Materia("Grafos", "#543EFE"));
        Long tarefaId = tarefas.save(new Tarefa("Lista 1", null, materia, null, null)).getId();

        mvc.perform(delete("/api/materias/{id}", materia.getId())).andExpect(status().isNoContent());

        assertThat(tarefas.findById(tarefaId)).hasValueSatisfying(t -> assertThat(t.getMateria()).isNull());
    }

    private void criar(String nome, String cor) throws Exception {
        mvc.perform(post("/api/materias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"%s\", \"cor\": \"%s\"}".formatted(nome, cor)))
                .andExpect(status().isCreated());
    }
}
