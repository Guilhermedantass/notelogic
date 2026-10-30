package dev.guilherme.notelogic.agenda;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.RelogioAjustavel;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.tarefa.Prioridade;
import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaRepository;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

/** "Hoje" é segunda, 2 de novembro de 2026 (relógio ajustável, como nos testes de recorrência). */
@Import({ TestcontainersConfiguration.class, RelogioAjustavel.Config.class })
@SpringBootTest
@AutoConfigureMockMvc
class ResumoDoDiaTests {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 2);

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    EventoRepository eventos;

    @Autowired
    CategoriaEventoRepository categorias;

    @Autowired
    TarefaRepository tarefas;

    @Autowired
    UsuarioRepository usuarios;

    Long eu;
    Long prova;
    Long treino;

    @BeforeEach
    void prepara() {
        tarefas.deleteAll();
        eventos.deleteAll();
        categorias.deleteAll();
        relogio.ajustar(Instant.parse("2026-11-02T13:00:00Z"));
        eu = Autenticacao.logarComo(usuarios, "eu@teste.dev");
        prova = categorias.save(new CategoriaEvento(eu, "Prova", "#E5484D", true)).getId();
        treino = categorias.save(new CategoriaEvento(eu, "Treino", "#22A06B", false)).getId();
    }

    @Test
    void agendaDeHojeEmOrdemComARotinaCalculada() throws Exception {
        evento("""
                {"titulo": "Aula", "dataInicio": "2026-11-02", "horaInicio": "19:00"}
                """);
        evento("""
                {"titulo": "Treino", "categoriaId": %d, "dataInicio": "2026-10-26", "horaInicio": "09:00",
                 "repeticao": {"tipo": "SEMANAL", "diasSemana": ["MONDAY"]}}
                """.formatted(treino));
        evento("""
                {"titulo": "Amanhã", "dataInicio": "2026-11-03", "diaInteiro": true}
                """);

        mvc.perform(get("/api/agenda/hoje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoje").value("2026-11-02"))
                .andExpect(jsonPath("$.agenda.length()").value(2))
                .andExpect(jsonPath("$.agenda[0].titulo").value("Treino"))
                .andExpect(jsonPath("$.agenda[1].titulo").value("Aula"))
                .andExpect(jsonPath("$.importantes.length()").value(0)); // nada de categoria com destaque
    }

    @Test
    void datasImportantesSaoProvasETarefasDePrioridadeAltaEm30Dias() throws Exception {
        evento("""
                {"titulo": "Prova de Grafos", "categoriaId": %d, "dataInicio": "2026-11-06", "horaInicio": "14:00"}
                """.formatted(prova));
        evento("""
                {"titulo": "Prova longe", "categoriaId": %d, "dataInicio": "2026-12-03", "diaInteiro": true}
                """.formatted(prova)); // 31 dias: fora
        evento("""
                {"titulo": "Treino", "categoriaId": %d, "dataInicio": "2026-11-04", "horaInicio": "09:00"}
                """.formatted(treino)); // categoria sem destaque: fora
        evento("""
                {"titulo": "Semana de provas", "categoriaId": %d, "dataInicio": "2026-10-30", "dataFim": "2026-11-03",
                 "diaInteiro": true}
                """.formatted(prova)); // já começou: conta como hoje
        tarefas.save(new Tarefa(eu, "Entregar lista", null, null, HOJE.plusDays(3), Prioridade.ALTA));
        tarefas.save(new Tarefa(eu, "Ler artigo", null, null, HOJE.plusDays(1), Prioridade.MEDIA)); // fora
        tarefas.save(new Tarefa(eu, "Atrasada", null, null, HOJE.minusDays(1), Prioridade.ALTA)); // fora

        mvc.perform(get("/api/agenda/hoje"))
                .andExpect(jsonPath("$.importantes.length()").value(3))
                .andExpect(jsonPath("$.importantes[0].titulo").value("Semana de provas"))
                .andExpect(jsonPath("$.importantes[0].diasAte").value(0))
                .andExpect(jsonPath("$.importantes[1].titulo").value("Entregar lista"))
                .andExpect(jsonPath("$.importantes[1].tipo").value("TAREFA"))
                .andExpect(jsonPath("$.importantes[1].rotulo").value("Tarefa"))
                .andExpect(jsonPath("$.importantes[1].diasAte").value(3))
                .andExpect(jsonPath("$.importantes[2].titulo").value("Prova de Grafos"))
                .andExpect(jsonPath("$.importantes[2].rotulo").value("Prova"))
                .andExpect(jsonPath("$.importantes[2].hora").value("14:00:00"))
                .andExpect(jsonPath("$.importantes[2].cor").value("#E5484D"));
    }

    private void evento(String json) throws Exception {
        mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }
}
