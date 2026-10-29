package dev.guilherme.notelogic.recorrencia;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.RelogioAjustavel;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.tarefa.TarefaRepository;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

/**
 * O relógio ajustável deixa "viver" vários dias num teste só: cria a regra na segunda, avança para a terça...
 * Mesmos @Import do teste do pomodoro: reaproveita aquele contexto do Spring.
 */
@Import({ TestcontainersConfiguration.class, RelogioAjustavel.Config.class })
@SpringBootTest
@AutoConfigureMockMvc
class RecorrenciaControllerTests {

    /** Segunda-feira, 2 de novembro de 2026, 10h em São Paulo. */
    static final Instant SEGUNDA_10H = Instant.parse("2026-11-02T13:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioAjustavel relogio;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    TarefaRepository tarefas;

    @Autowired
    RecorrenciaRepository recorrencias;

    @BeforeEach
    void limpa() {
        tarefas.deleteAll();
        recorrencias.deleteAll();
        relogio.ajustar(SEGUNDA_10H);
        Autenticacao.logarComo(usuarios, "eu@teste.dev");
    }

    @Test
    void diariaCriaATarefaDeHojeSemDuplicar() throws Exception {
        criar(regra("DIARIA", "")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumo").value("Todo dia"))
                .andExpect(jsonPath("$.acumula").value(false));

        pendentes().andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Listening"))
                .andExpect(jsonPath("$[0].prazo").value("2026-11-02"))
                .andExpect(jsonPath("$[0].recorrencia.descricao").value("Todo dia"));

        pendentes().andExpect(jsonPath("$.length()").value(1)); // listar de novo não cria outra
    }

    @Test
    void concluirHojeMostraAProxima() throws Exception {
        criar(regra("DIARIA", ""));
        long hoje = idDaPrimeiraPendente();

        mvc.perform(patch("/api/tarefas/{id}/concluir", hoje)).andExpect(status().isOk());

        pendentes().andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prazo").value("2026-11-03")); // só a próxima, e não a semana toda
    }

    @Test
    void expiraPorPadraoQuandoODiaPassa() throws Exception {
        criar(regra("DIARIA", ""));

        relogio.avancar(Duration.ofDays(3)); // ficou três dias sem abrir o app

        pendentes().andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prazo").value("2026-11-05")); // só a de hoje; a de segunda expirou
    }

    @Test
    void acumulaViraAtrasada() throws Exception {
        criar(regra("DIARIA", "", true));

        relogio.avancar(Duration.ofDays(2));

        pendentes().andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].prazo", containsInAnyOrder("2026-11-02", "2026-11-03", "2026-11-04")));
    }

    @Test
    void semanalForaDoDiaMostraAProximaData() throws Exception {
        // hoje é segunda; a regra é terça e quinta
        criar(regra("SEMANAL", "\"diasSemana\": [\"TUESDAY\", \"THURSDAY\"]"))
                .andExpect(jsonPath("$.resumo").value("Toda terça e quinta"));

        pendentes().andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prazo").value("2026-11-03"));
    }

    @Test
    void editarEstaEAsProximasMudaSoDaquiPraFrente() throws Exception {
        long regra = id(criar(regra("DIARIA", "")));
        mvc.perform(patch("/api/tarefas/{id}/concluir", idDaPrimeiraPendente())); // segunda: feita
        pendentes(); // gera a de terça

        json(put("/api/recorrencias/{id}", regra).param("aPartirDe", "2026-11-03"),
                """
                {"titulo": "Listening (45 min)", "regra": {"tipo": "DIARIA", "inicio": "2026-11-02"}}
                """).andExpect(status().isOk());

        pendentes().andExpect(jsonPath("$[0].titulo").value("Listening (45 min)"));
        mvc.perform(get("/api/tarefas").param("concluida", "true"))
                .andExpect(jsonPath("$[0].titulo").value("Listening")); // a de segunda continua como foi feita
    }

    @Test
    void encerrarAPartirDeHojeParaDeGerar() throws Exception {
        long regra = id(criar(regra("DIARIA", "")));

        mvc.perform(delete("/api/recorrencias/{id}", regra).param("aPartirDe", "2026-11-02"))
                .andExpect(status().isNoContent());

        pendentes().andExpect(jsonPath("$.length()").value(0));
        relogio.avancar(Duration.ofDays(5));
        pendentes().andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/recorrencias")).andExpect(jsonPath("$[0].encerrada").value(true));
    }

    @Test
    void constanciaContaFeitasDeEsperadas() throws Exception {
        criar(regra("DIARIA", ""));
        mvc.perform(patch("/api/tarefas/{id}/concluir", idDaPrimeiraPendente())); // segunda: feita
        relogio.avancar(Duration.ofDays(1));
        pendentes(); // terça: não faz
        relogio.avancar(Duration.ofDays(1));
        mvc.perform(patch("/api/tarefas/{id}/concluir", idDaPrimeiraPendente())); // quarta: feita

        mvc.perform(get("/api/recorrencias"))
                .andExpect(jsonPath("$[0].feitas").value(2))
                .andExpect(jsonPath("$[0].esperadas").value(3));
    }

    @Test
    void regraIncoerenteDa422EFormatoErradoDa400() throws Exception {
        criar(regra("SEMANAL", "\"diasSemana\": []"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Escolha pelo menos um dia da semana"));

        criar(regra("MENSAL", "\"diaDoMes\": 40")).andExpect(status().isBadRequest());
    }

    @Test
    void outroUsuarioNaoMexeNaRegra() throws Exception {
        long regra = id(criar(regra("DIARIA", "")));

        Autenticacao.logarComo(usuarios, "outro@teste.dev");
        pendentes().andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/recorrencias")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/recorrencias/{id}", regra).param("aPartirDe", "2026-11-02"))
                .andExpect(status().isNotFound());
    }

    // ---- ajudantes ----

    private static String regra(String tipo, String extra) {
        return regra(tipo, extra, false);
    }

    private static String regra(String tipo, String extra, boolean acumula) {
        String campos = extra.isBlank() ? "" : ", " + extra;
        return """
                {"titulo": "Listening", "acumula": %s, "regra": {"tipo": "%s"%s}}
                """.formatted(acumula, tipo, campos);
    }

    private ResultActions criar(String corpo) throws Exception {
        return json(post("/api/recorrencias"), corpo);
    }

    private ResultActions pendentes() throws Exception {
        return mvc.perform(get("/api/tarefas").param("concluida", "false")).andExpect(status().isOk());
    }

    private long idDaPrimeiraPendente() throws Exception {
        String json = pendentes().andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$[0].id")).longValue();
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
            String corpo) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static long id(ResultActions resultado) throws Exception {
        String json = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
