package dev.guilherme.notelogic.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FlashcardControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    BaralhoRepository baralhos;

    @Autowired
    CartaoRepository cartoes;

    @Autowired
    RevisaoRepository revisoes;

    @Autowired
    Clock clock;

    LocalDate hoje;

    @Autowired
    UsuarioRepository usuarios;

    Long eu;

    @BeforeEach
    void limpa() {
        baralhos.deleteAll(); // cascade apaga cartões e revisões
        hoje = LocalDate.now(clock);
        eu = Autenticacao.logarComo(usuarios, "eu@teste.dev");
    }

    @Test
    void cartaoNovoEntraNaFilaDeHoje() throws Exception {
        long baralho = criarBaralho("Inglês");
        criarCartao(baralho, "apple", "maçã");
        criarCartao(baralho, "book", "livro");

        mvc.perform(get("/api/baralhos/{id}/estudo", baralho))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].frente").value("apple"))
                .andExpect(jsonPath("$[0].proximaRevisao").value(hoje.toString()));
    }

    @Test
    void responderBomTiraDaFilaEAgendaParaAmanha() throws Exception {
        long baralho = criarBaralho("Inglês");
        long cartao = criarCartao(baralho, "apple", "maçã");

        revisar(cartao, "BOM")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervaloDias").value(1))
                .andExpect(jsonPath("$.proximaRevisao").value(hoje.plusDays(1).toString()));

        mvc.perform(get("/api/baralhos/{id}/estudo", baralho)).andExpect(jsonPath("$.length()").value(0));
        assertThat(revisoes.count()).isEqualTo(1);
    }

    @Test
    void responderErreiMantemNaFila() throws Exception {
        long baralho = criarBaralho("Inglês");
        long cartao = criarCartao(baralho, "apple", "maçã");

        revisar(cartao, "ERREI").andExpect(jsonPath("$.proximaRevisao").value(hoje.toString()));

        mvc.perform(get("/api/baralhos/{id}/estudo", baralho)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listaBaralhosComContagens() throws Exception {
        long ingles = criarBaralho("Inglês");
        criarBaralho("Grafos");
        long c1 = criarCartao(ingles, "apple", "maçã");
        criarCartao(ingles, "book", "livro");
        revisar(c1, "FACIL");

        mvc.perform(get("/api/baralhos"))
                .andExpect(jsonPath("$[0].nome").value("Grafos"))
                .andExpect(jsonPath("$[0].totalCartoes").value(0))
                .andExpect(jsonPath("$[1].nome").value("Inglês"))
                .andExpect(jsonPath("$[1].totalCartoes").value(2))
                .andExpect(jsonPath("$[1].paraRevisarHoje").value(1));
    }

    @Test
    void respostaInvalidaDa400() throws Exception {
        long cartao = criarCartao(criarBaralho("Inglês"), "apple", "maçã");

        mvc.perform(post("/api/cartoes/{id}/revisao", cartao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resposta\": \"TALVEZ\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/cartoes/{id}/revisao", cartao).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.resposta").exists());
    }

    @Test
    void apagarBaralhoApagaCartoesERevisoes() throws Exception {
        long baralho = criarBaralho("Inglês");
        long cartao = criarCartao(baralho, "apple", "maçã");
        revisar(cartao, "BOM");

        mvc.perform(delete("/api/baralhos/{id}", baralho)).andExpect(status().isNoContent());

        assertThat(cartoes.count()).isZero();
        assertThat(revisoes.count()).isZero();
    }

    @Test
    void cartaoEmBaralhoInexistenteDa404() throws Exception {
        mvc.perform(post("/api/baralhos/999/cartoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"frente\": \"a\", \"verso\": \"b\"}"))
                .andExpect(status().isNotFound());
    }

    private long criarBaralho(String nome) throws Exception {
        String json = mvc.perform(post("/api/baralhos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"%s\"}".formatted(nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long criarCartao(long baralho, String frente, String verso) throws Exception {
        String json = mvc.perform(post("/api/baralhos/{id}/cartoes", baralho).contentType(MediaType.APPLICATION_JSON)
                .content("{\"frente\": \"%s\", \"verso\": \"%s\"}".formatted(frente, verso)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions revisar(long cartao, String resposta) throws Exception {
        return mvc.perform(post("/api/cartoes/{id}/revisao", cartao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resposta\": \"%s\"}".formatted(resposta)));
    }
}
