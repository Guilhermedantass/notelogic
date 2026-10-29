package dev.guilherme.notelogic.pomodoro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.RelogioAjustavel;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaRepository;
import dev.guilherme.notelogic.tarefa.Tarefa;
import dev.guilherme.notelogic.tarefa.TarefaRepository;

@Import({ TestcontainersConfiguration.class, RelogioAjustavel.Config.class })
@SpringBootTest
@AutoConfigureMockMvc
class SessaoFocoControllerTests {

    /** 10h da manhã em São Paulo, segunda-feira. */
    static final Instant INICIO = Instant.parse("2026-11-02T13:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    SessaoFocoRepository sessoes;

    @Autowired
    TarefaRepository tarefas;

    @Autowired
    MateriaRepository materias;

    @BeforeEach
    void limpa() {
        sessoes.deleteAll();
        tarefas.deleteAll();
        materias.deleteAll();
        relogio.ajustar(INICIO);
    }

    @Test
    void focoSemDuracaoUsa25Minutos() throws Exception {
        iniciar("{\"tipo\": \"FOCO\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.duracaoPlanejadaMin").value(25))
                .andExpect(jsonPath("$.iniciadaEm").value("2026-11-02T13:00:00Z"))
                .andExpect(jsonPath("$.fimPrevisto").value("2026-11-02T13:25:00Z"));
    }

    @Test
    void sessaoComTarefaHerdaAMateriaDela() throws Exception {
        Materia grafos = materias.save(new Materia("Grafos", "#543EFE"));
        Long tarefa = tarefas.save(new Tarefa("Lista 3", null, grafos, null, null)).getId();

        iniciar("{\"tipo\": \"FOCO\", \"tarefaId\": %d}".formatted(tarefa))
                .andExpect(jsonPath("$.tarefa.titulo").value("Lista 3"))
                .andExpect(jsonPath("$.materia.nome").value("Grafos"));
    }

    @Test
    void naoDeixaDuasSessoesAoMesmoTempo() throws Exception {
        iniciar("{\"tipo\": \"FOCO\"}").andExpect(status().isCreated());

        iniciar("{\"tipo\": \"PAUSA_CURTA\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma sessão em andamento"));
    }

    @Test
    void concluirAtrasadoContaSoATempoPlanejado() throws Exception {
        long id = idDe(iniciar("{\"tipo\": \"FOCO\"}"));

        relogio.avancar(Duration.ofMinutes(31)); // você foi buscar café e voltou depois do alarme

        mvc.perform(patch("/api/pomodoro/sessoes/{id}/concluir", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.segundosFocados").value(25 * 60))
                .andExpect(jsonPath("$.finalizadaEm").value("2026-11-02T13:31:00Z"));
    }

    @Test
    void interromperGuardaOTempoQueDeu() throws Exception {
        long id = idDe(iniciar("{\"tipo\": \"FOCO\"}"));

        relogio.avancar(Duration.ofMinutes(10).plusSeconds(30));

        mvc.perform(patch("/api/pomodoro/sessoes/{id}/interromper", id))
                .andExpect(jsonPath("$.status").value("INTERROMPIDA"))
                .andExpect(jsonPath("$.segundosFocados").value(630));
    }

    @Test
    void naoFinalizaDuasVezes() throws Exception {
        long id = idDe(iniciar("{\"tipo\": \"FOCO\"}"));
        mvc.perform(patch("/api/pomodoro/sessoes/{id}/interromper", id)).andExpect(status().isOk());

        mvc.perform(patch("/api/pomodoro/sessoes/{id}/concluir", id)).andExpect(status().isConflict());
    }

    @Test
    void atualDa204SemSessaoE200ComSessao() throws Exception {
        mvc.perform(get("/api/pomodoro/sessoes/atual")).andExpect(status().isNoContent());

        iniciar("{\"tipo\": \"PAUSA_LONGA\"}");

        mvc.perform(get("/api/pomodoro/sessoes/atual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PAUSA_LONGA"))
                .andExpect(jsonPath("$.duracaoPlanejadaMin").value(15));
    }

    @Test
    void pausaLigadaATarefaDa422() throws Exception {
        Long tarefa = tarefas.save(new Tarefa("Lista 3", null, null, null, null)).getId();

        iniciar("{\"tipo\": \"PAUSA_CURTA\", \"tarefaId\": %d}".formatted(tarefa))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void duracaoForaDoLimiteDa400() throws Exception {
        iniciar("{\"tipo\": \"FOCO\", \"duracaoMin\": 500}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.duracaoMin").exists());
    }

    @Test
    void listaPorDiaNoFusoDoApp() throws Exception {
        // 23h30 de domingo em São Paulo = 02h30 UTC de segunda: tem que contar como DOMINGO
        relogio.ajustar(Instant.parse("2026-11-02T02:30:00Z"));
        long domingo = idDe(iniciar("{\"tipo\": \"FOCO\"}"));
        mvc.perform(patch("/api/pomodoro/sessoes/{id}/concluir", domingo));

        relogio.ajustar(INICIO);
        iniciar("{\"tipo\": \"FOCO\"}");

        mvc.perform(get("/api/pomodoro/sessoes").param("de", "2026-11-01").param("ate", "2026-11-01"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(domingo));

        mvc.perform(get("/api/pomodoro/sessoes")) // sem parâmetros = hoje (segunda)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("EM_ANDAMENTO"));
    }

    private ResultActions iniciar(String json) throws Exception {
        return mvc.perform(post("/api/pomodoro/sessoes").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idDe(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
