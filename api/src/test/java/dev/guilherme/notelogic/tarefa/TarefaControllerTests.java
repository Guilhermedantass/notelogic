package dev.guilherme.notelogic.tarefa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.usuario.UsuarioRepository;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TarefaControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    TarefaRepository repository;

    @Autowired
    MateriaRepository materias;

    @Autowired
    UsuarioRepository usuarios;

    Long eu;

    @BeforeEach
    void limpa() {
        repository.deleteAll();
        materias.deleteAll();
        eu = Autenticacao.logarComo(usuarios, "eu@teste.dev");
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
        Long id = repository.save(new Tarefa(eu, "Antigo", null, null, null, null)).getId();

        mvc.perform(put("/api/tarefas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Novo", "descricao": "editada"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Novo"));

        mvc.perform(patch("/api/tarefas/{id}/concluir", id))
                .andExpect(jsonPath("$.concluida").value(true))
                .andExpect(jsonPath("$.concluidaEm").isNotEmpty());

        mvc.perform(patch("/api/tarefas/{id}/reabrir", id))
                .andExpect(jsonPath("$.concluida").value(false))
                .andExpect(jsonPath("$.concluidaEm").doesNotExist());
    }

    @Test
    void remove() throws Exception {
        Long id = repository.save(new Tarefa(eu, "Apagar", null, null, null, null)).getId();

        mvc.perform(delete("/api/tarefas/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void criaComMateriaPrazoEPrioridade() throws Exception {
        Long materiaId = materias.save(new Materia(eu, "Grafos", "#543EFE")).getId();

        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Lista de exercícios", "materiaId": %d, "prazo": "2026-11-05", "prioridade": "ALTA"}
                """.formatted(materiaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.materia.nome").value("Grafos"))
                .andExpect(jsonPath("$.materia.cor").value("#543EFE"))
                .andExpect(jsonPath("$.prazo").value("2026-11-05"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));
    }

    @Test
    void semPrioridadeFicaMedia() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Qualquer coisa"}
                """))
                .andExpect(jsonPath("$.prioridade").value("MEDIA"))
                .andExpect(jsonPath("$.materia").doesNotExist());
    }

    @Test
    void materiaInexistenteDa404() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Órfã", "materiaId": 999}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Matéria 999 não encontrado(a)"));
    }

    @Test
    void filtraPorMateriaEStatusComPendentesPrimeiro() throws Exception {
        Materia grafos = materias.save(new Materia(eu, "Grafos", "#543EFE"));
        Materia ingles = materias.save(new Materia(eu, "Inglês", "#22AA55"));
        repository.save(new Tarefa(eu, "Sem prazo", null, grafos, null, null));
        repository.save(new Tarefa(eu, "Prazo cedo", null, grafos, LocalDate.of(2026, 11, 1), null));
        Tarefa feita = new Tarefa(eu, "Já feita", null, grafos, LocalDate.of(2026, 10, 1), null);
        feita.concluir(Instant.now());
        repository.save(feita);
        repository.save(new Tarefa(eu, "Vocabulário", null, ingles, null, null));

        mvc.perform(get("/api/tarefas").param("materiaId", grafos.getId().toString()))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].titulo").value("Prazo cedo"))
                .andExpect(jsonPath("$[1].titulo").value("Sem prazo"))
                .andExpect(jsonPath("$[2].titulo").value("Já feita"));

        mvc.perform(get("/api/tarefas").param("concluida", "false"))
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/api/tarefas").param("materiaId", grafos.getId().toString()).param("concluida", "true"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Já feita"));
    }
}
