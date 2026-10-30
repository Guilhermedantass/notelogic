package dev.guilherme.notelogic.agenda;

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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

/** "Treinar toda segunda e quarta às 9h", começando na segunda, 2 de novembro de 2026. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RepeticaoDeEventosTests {

    static final String TREINO = """
            {"titulo": "Treino", "local": "Academia", "dataInicio": "2026-11-02", "horaInicio": "09:00",
             "horaFim": "10:00", "repeticao": {"tipo": "SEMANAL", "diasSemana": ["MONDAY", "WEDNESDAY"]}}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    EventoRepository eventos;

    @Autowired
    UsuarioRepository usuarios;

    @BeforeEach
    void limpa() {
        eventos.deleteAll(); // as exceções vão junto (on delete cascade)
        Autenticacao.logarComo(usuarios, "eu@teste.dev");
    }

    @Test
    void asOcorrenciasSaoCalculadasParaOPeriodoPedido() throws Exception {
        criar(TREINO).andExpect(status().isCreated())
                .andExpect(jsonPath("$.repeticaoResumo").value("Toda segunda e quarta"))
                .andExpect(jsonPath("$.repeticao.inicio").value("2026-11-02"));

        semana("2026-11-09", "2026-11-15")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dataInicio").value("2026-11-09"))
                .andExpect(jsonPath("$[0].dataOcorrencia").value("2026-11-09"))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$[0].local").value("Academia"))
                .andExpect(jsonPath("$[1].dataInicio").value("2026-11-11"));

        semana("2026-10-26", "2026-11-01").andExpect(jsonPath("$.length()").value(0)); // antes de começar
    }

    @Test
    void misturaAvulsosEOcorrenciasEmOrdemDeHorario() throws Exception {
        criar(TREINO);
        criar("""
                {"titulo": "Prova", "dataInicio": "2026-11-04", "horaInicio": "08:00"}
                """);
        criar("""
                {"titulo": "Feriado", "dataInicio": "2026-11-04", "diaInteiro": true}
                """);

        semana("2026-11-04", "2026-11-04")
                .andExpect(jsonPath("$[0].titulo").value("Feriado"))
                .andExpect(jsonPath("$[0].dataOcorrencia").doesNotExist())
                .andExpect(jsonPath("$[1].titulo").value("Prova"))
                .andExpect(jsonPath("$[2].titulo").value("Treino"));
    }

    @Test
    void respeitaOFimEOLimiteDeVezes() throws Exception {
        criar("""
                {"titulo": "Monitoria", "dataInicio": "2026-11-02", "horaInicio": "14:00",
                 "repeticao": {"tipo": "DIARIA", "maxOcorrencias": 3}}
                """);
        criar("""
                {"titulo": "Aula", "dataInicio": "2026-11-02", "horaInicio": "19:00",
                 "repeticao": {"tipo": "DIAS_UTEIS", "fim": "2026-11-03"}}
                """);

        // pedindo a partir do dia 03: a monitoria ainda conta as vezes desde o dia 02
        semana("2026-11-03", "2026-11-08")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.titulo == 'Monitoria')].dataInicio").value(
                        org.hamcrest.Matchers.contains("2026-11-03", "2026-11-04")))
                .andExpect(jsonPath("$[?(@.titulo == 'Aula')].dataInicio").value(
                        org.hamcrest.Matchers.contains("2026-11-03")));
    }

    @Test
    void soEventosDeUmDiaSeRepetem() throws Exception {
        criar("""
                {"titulo": "Plantão", "dataInicio": "2026-11-06", "dataFim": "2026-11-08", "diaInteiro": true,
                 "repeticao": {"tipo": "SEMANAL", "diasSemana": ["FRIDAY"]}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Só eventos de um dia podem se repetir"));

        criar("""
                {"titulo": "X", "dataInicio": "2026-11-06", "diaInteiro": true, "repeticao": {"tipo": "SEMANAL"}}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Escolha pelo menos um dia da semana"));
    }

    @Test
    void soEstaCancelada() throws Exception {
        Long id = id(criar(TREINO));

        mvc.perform(delete("/api/eventos/{id}/ocorrencias/{dia}", id, "2026-11-04")).andExpect(status().isNoContent());

        semana("2026-11-02", "2026-11-08")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dataInicio").value("2026-11-02"));
    }

    @Test
    void soEstaMovidaParaOutroDiaEHorario() throws Exception {
        Long id = id(criar(TREINO));

        // o treino de quarta 04/11 vai para sábado 07/11 às 18h, em outro lugar
        alterar(id, "2026-11-04", """
                {"data": "2026-11-07", "horaInicio": "18:00", "local": "Parque"}
                """).andExpect(status().isNoContent());

        semana("2026-11-02", "2026-11-08")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].dataInicio").value("2026-11-07"))
                .andExpect(jsonPath("$[1].dataOcorrencia").value("2026-11-04"))
                .andExpect(jsonPath("$[1].horaInicio").value("18:00:00"))
                .andExpect(jsonPath("$[1].horaFim").doesNotExist())
                .andExpect(jsonPath("$[1].local").value("Parque"))
                .andExpect(jsonPath("$[1].alterada").value(true));

        // movida para a semana seguinte: some desta e aparece na outra
        alterar(id, "2026-11-04", """
                {"data": "2026-11-10", "horaInicio": "07:00"}
                """);
        semana("2026-11-02", "2026-11-08").andExpect(jsonPath("$.length()").value(1));
        semana("2026-11-09", "2026-11-15")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].dataInicio").value("2026-11-10"));
    }

    @Test
    void soEstaNumDiaSemOcorrenciaOuNumEventoAvulsoDa422() throws Exception {
        Long treino = id(criar(TREINO));
        Long avulso = id(criar("""
                {"titulo": "Prova", "dataInicio": "2026-11-04", "horaInicio": "08:00"}
                """));

        mvc.perform(delete("/api/eventos/{id}/ocorrencias/{dia}", treino, "2026-11-03")) // terça
                .andExpect(status().isUnprocessableContent());
        mvc.perform(delete("/api/eventos/{id}/ocorrencias/{dia}", avulso, "2026-11-04"))
                .andExpect(status().isUnprocessableContent());
        alterar(treino, "2026-11-04", """
                {"data": "2026-11-04", "horaInicio": "10:00", "horaFim": "09:00"}
                """).andExpect(status().isUnprocessableContent());
    }

    @Test
    void estaEAsProximasDivideASerie() throws Exception {
        Long id = id(criar(TREINO));
        alterar(id, "2026-11-11", """
                {"data": "2026-11-11", "horaInicio": "07:00"}
                """); // exceção que fica na parte que acaba: deve sumir

        // a partir de segunda 09/11, o treino passa a ser às 18h
        mvc.perform(put("/api/eventos/{id}", id).param("aPartirDe", "2026-11-09")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TREINO.replace("2026-11-02", "2026-11-09").replace("09:00", "18:00").replace("10:00", "19:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(id.intValue())))
                .andExpect(jsonPath("$.dataInicio").value("2026-11-09"));

        semana("2026-11-02", "2026-11-15")
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00")) // 02/11, a série antiga
                .andExpect(jsonPath("$[1].horaInicio").value("09:00:00")) // 04/11
                .andExpect(jsonPath("$[2].horaInicio").value("18:00:00")) // 09/11, a série nova
                .andExpect(jsonPath("$[3].horaInicio").value("18:00:00")) // 11/11, sem a exceção antiga
                .andExpect(jsonPath("$[3].alterada").value(false));

        mvc.perform(get("/api/eventos/{id}", id))
                .andExpect(jsonPath("$.repeticao.fim").value("2026-11-08"));
    }

    @Test
    void dividirUmaSerieComLimiteDeVezesMantemOTotal() throws Exception {
        String monitoria = """
                {"titulo": "Monitoria", "dataInicio": "2026-11-02", "horaInicio": "14:00",
                 "repeticao": {"tipo": "DIARIA", "maxOcorrencias": 5}}
                """;
        Long id = id(criar(monitoria));

        // depois de 2 vezes (02 e 03/11), muda o horário: restam 3
        mvc.perform(put("/api/eventos/{id}", id).param("aPartirDe", "2026-11-04")
                .contentType(MediaType.APPLICATION_JSON)
                .content(monitoria.replace("2026-11-02", "2026-11-04").replace("14:00", "16:00")))
                .andExpect(jsonPath("$.repeticao.maxOcorrencias").value(3));

        semana("2026-11-02", "2026-11-15").andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void editarAPartirDoPrimeiroDiaEditaASerieInteira() throws Exception {
        Long id = id(criar(TREINO));

        mvc.perform(put("/api/eventos/{id}", id).param("aPartirDe", "2026-11-02")
                .contentType(MediaType.APPLICATION_JSON).content(TREINO.replace("Treino", "Treino de perna")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()));

        semana("2026-11-02", "2026-11-08").andExpect(jsonPath("$[0].titulo").value("Treino de perna"));
    }

    @Test
    void pararDeRepetirAPartirDeUmDiaEApagarTudo() throws Exception {
        Long id = id(criar(TREINO));

        mvc.perform(delete("/api/eventos/{id}", id).param("aPartirDe", "2026-11-09")).andExpect(status().isNoContent());
        semana("2026-11-02", "2026-11-15").andExpect(jsonPath("$.length()").value(2));

        mvc.perform(delete("/api/eventos/{id}", id)).andExpect(status().isNoContent());
        semana("2026-11-02", "2026-11-15").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deixarDeRepetirVoltaASerUmEventoSo() throws Exception {
        Long id = id(criar(TREINO));
        mvc.perform(delete("/api/eventos/{id}/ocorrencias/{dia}", id, "2026-11-02"));

        mvc.perform(put("/api/eventos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Treino", "dataInicio": "2026-11-02", "horaInicio": "09:00"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repeticao").doesNotExist());

        // o cancelamento antigo não vale mais: o evento único do dia 02 aparece
        semana("2026-11-02", "2026-11-08")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dataOcorrencia").doesNotExist());
    }

    private ResultActions criar(String json) throws Exception {
        return mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions alterar(Long id, String dia, String json) throws Exception {
        return mvc.perform(put("/api/eventos/{id}/ocorrencias/{dia}", id, dia)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions semana(String de, String ate) throws Exception {
        return mvc.perform(get("/api/eventos").param("de", de).param("ate", ate)).andExpect(status().isOk());
    }

    private static Long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
