package dev.guilherme.notelogic.tarefa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TarefaControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TarefaRepository repository;

    @BeforeEach
    void limpa() {
        repository.deleteAll();
    }

    @Test
    void criaELista() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"titulo": "Estudar JPA", "descricao": "Rodada 5"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Estudar JPA"))
                .andExpect(jsonPath("$.concluida").value(false));

        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void recusaTituloEmBrancoComProblemDetail() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "   "}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.titulo").exists());
    }

    @Test
    void recusaTituloLongoDemais() throws Exception {
        String titulo = "a".repeat(201);
        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\": \"" + titulo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").exists());
    }

    @Test
    void idInexistenteDa404() throws Exception {
        mvc.perform(get("/api/tarefas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Tarefa 999 não encontrado(a)"));

        mvc.perform(delete("/api/tarefas/999")).andExpect(status().isNotFound());
    }

    @Test
    void editaConcluiEReabre() throws Exception {
        Long id = repository.save(new Tarefa("Antigo", null)).getId();

        mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Novo", "descricao": "editada"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Novo"));

        mvc.perform(patch("/api/tarefas/{id}/concluir", id))
                .andExpect(jsonPath("$.concluida").value(true));

        mvc.perform(patch("/api/tarefas/{id}/reabrir", id))
                .andExpect(jsonPath("$.concluida").value(false));
    }

    @Test
    void remove() throws Exception {
        Long id = repository.save(new Tarefa("Apagar", null)).getId();

        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
    }
}
