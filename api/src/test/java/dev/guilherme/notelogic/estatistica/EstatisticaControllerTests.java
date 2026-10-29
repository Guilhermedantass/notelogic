package dev.guilherme.notelogic.estatistica;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.RelogioAjustavel;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.flashcard.BaralhoRepository;
import dev.guilherme.notelogic.materia.MateriaRepository;
import dev.guilherme.notelogic.pomodoro.SessaoFocoRepository;
import dev.guilherme.notelogic.tarefa.TarefaRepository;

/**
 * Monta três dias de estudo pela própria API, avançando o relógio, e confere o painel.
 * Mesmos @Import do teste do pomodoro: o Spring reaproveita aquele contexto em vez de subir outro.
 */
@Import({ TestcontainersConfiguration.class, RelogioAjustavel.Config.class })
@SpringBootTest
@AutoConfigureMockMvc
class EstatisticaControllerTests {

    /** Segunda-feira, 10h em São Paulo. */
    static final Instant SEGUNDA_10H = Instant.parse("2026-11-02T13:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    SessaoFocoRepository sessoes;

    @Autowired
    BaralhoRepository baralhos;

    @Autowired
    TarefaRepository tarefas;

    @Autowired
    MateriaRepository materias;

    @BeforeEach
    void limpa() {
        sessoes.deleteAll();
        baralhos.deleteAll();
        tarefas.deleteAll();
        materias.deleteAll();
    }

    @Test
    void painelDeTresDiasDeEstudo() throws Exception {
        long grafos = id(post("/api/materias", "{\"nome\": \"Grafos\", \"cor\": \"#543EFE\"}"));
        long baralho = id(post("/api/baralhos", "{\"nome\": \"Inglês\"}"));

        // sábado 10h: 25 min de foco em Grafos
        relogio.ajustar(SEGUNDA_10H.minus(Duration.ofDays(2)));
        focar("{\"tipo\": \"FOCO\", \"materiaId\": %d}".formatted(grafos), Duration.ofMinutes(25), "concluir");

        // domingo 23h30: 10 min sem matéria, interrompido; cria 2 cartões e erra o primeiro
        relogio.ajustar(Instant.parse("2026-11-02T02:30:00Z"));
        focar("{\"tipo\": \"FOCO\"}", Duration.ofMinutes(10), "interromper");
        long apple = id(post("/api/baralhos/%d/cartoes".formatted(baralho), "{\"frente\": \"apple\", \"verso\": \"maçã\"}"));
        long book = id(post("/api/baralhos/%d/cartoes".formatted(baralho), "{\"frente\": \"book\", \"verso\": \"livro\"}"));
        post("/api/cartoes/%d/revisao".formatted(apple), "{\"resposta\": \"ERREI\"}");

        // segunda 10h: 25 min em Grafos, acerta os dois cartões, conclui uma tarefa e cria um cartão novo
        relogio.ajustar(SEGUNDA_10H);
        focar("{\"tipo\": \"FOCO\", \"materiaId\": %d}".formatted(grafos), Duration.ofMinutes(25), "concluir");
        post("/api/cartoes/%d/revisao".formatted(apple), "{\"resposta\": \"BOM\"}");
        post("/api/cartoes/%d/revisao".formatted(book), "{\"resposta\": \"FACIL\"}");
        long tarefa = id(post("/api/tarefas", "{\"titulo\": \"Lista 4\"}"));
        mvc.perform(patch("/api/tarefas/{id}/concluir", tarefa)).andExpect(status().isOk());
        post("/api/baralhos/%d/cartoes".formatted(baralho), "{\"frente\": \"cat\", \"verso\": \"gato\"}");

        mvc.perform(get("/api/estatisticas").param("dias", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoje.minutosFocados").value(25))
                .andExpect(jsonPath("$.hoje.cartoesRevisados").value(2))
                .andExpect(jsonPath("$.hoje.tarefasConcluidas").value(1))
                .andExpect(jsonPath("$.hoje.cartoesParaRevisar").value(1)) // só o "cat", criado hoje
                .andExpect(jsonPath("$.sequencia").value(3))
                .andExpect(jsonPath("$.taxaDeAcerto").value(closeTo(2.0 / 3, 0.0001)))
                .andExpect(jsonPath("$.dias.length()").value(7))
                .andExpect(jsonPath("$.dias[6].data").value("2026-11-02"))
                .andExpect(jsonPath("$.dias[5].data").value("2026-11-01"))
                .andExpect(jsonPath("$.dias[5].minutosFocados").value(10)) // 23h30 de domingo conta no domingo
                .andExpect(jsonPath("$.dias[5].cartoesRevisados").value(1))
                .andExpect(jsonPath("$.dias[4].minutosFocados").value(25))
                .andExpect(jsonPath("$.dias[0].minutosFocados").value(0))
                .andExpect(jsonPath("$.focoPorMateria.length()").value(1))
                .andExpect(jsonPath("$.focoPorMateria[0].materia.nome").value("Grafos"))
                .andExpect(jsonPath("$.focoPorMateria[0].minutos").value(50));
    }

    @Test
    void semNadaTudoZeradoETaxaNula() throws Exception {
        relogio.ajustar(SEGUNDA_10H);

        mvc.perform(get("/api/estatisticas"))
                .andExpect(jsonPath("$.dias.length()").value(30))
                .andExpect(jsonPath("$.sequencia").value(0))
                .andExpect(jsonPath("$.taxaDeAcerto").doesNotExist())
                .andExpect(jsonPath("$.focoPorMateria.length()").value(0));
    }

    @Test
    void diasForaDoLimiteDa400() throws Exception {
        mvc.perform(get("/api/estatisticas").param("dias", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/estatisticas").param("dias", "1000")).andExpect(status().isBadRequest());
    }

    private void focar(String json, Duration duracao, String fim) throws Exception {
        long sessao = id(post("/api/pomodoro/sessoes", json));
        relogio.avancar(duracao);
        mvc.perform(patch("/api/pomodoro/sessoes/{id}/" + fim, sessao)).andExpect(status().isOk());
    }

    private String post(String url, String json) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
