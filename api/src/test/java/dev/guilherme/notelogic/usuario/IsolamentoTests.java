package dev.guilherme.notelogic.usuario;

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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.TestcontainersConfiguration;

/**
 * A garantia mais importante do login: a Ana cria os dados dela, o Beto entra e não consegue
 * ver, editar, apagar nem usar nada disso. Para o Beto, os ids da Ana simplesmente "não existem" (404).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class IsolamentoTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    long materiaDaAna;
    long tarefaDaAna;
    long baralhoDaAna;
    long cartaoDaAna;
    long sessaoDaAna;

    @BeforeEach
    void anaCriaOsDadosDela() throws Exception {
        usuarios.deleteAll();
        Autenticacao.logarComo(usuarios, "ana@teste.dev");

        materiaDaAna = id(json(post("/api/materias"), "{\"nome\": \"Grafos\", \"cor\": \"#543EFE\"}"));
        tarefaDaAna = id(json(post("/api/tarefas"), "{\"titulo\": \"Lista da Ana\", \"materiaId\": %d}".formatted(materiaDaAna)));
        baralhoDaAna = id(json(post("/api/baralhos"), "{\"nome\": \"Baralho da Ana\"}"));
        cartaoDaAna = id(json(post("/api/baralhos/%d/cartoes".formatted(baralhoDaAna)), "{\"frente\": \"a\", \"verso\": \"b\"}"));
        sessaoDaAna = id(json(post("/api/pomodoro/sessoes"), "{\"tipo\": \"FOCO\"}"));

        Autenticacao.logarComo(usuarios, "beto@teste.dev");
    }

    @Test
    void betoNaoVeNadaDaAnaNasListas() throws Exception {
        mvc.perform(get("/api/materias")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tarefas")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/baralhos")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/pomodoro/sessoes")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/pomodoro/sessoes/atual")).andExpect(status().isNoContent());
        mvc.perform(get("/api/estatisticas"))
                .andExpect(jsonPath("$.hoje.cartoesParaRevisar").value(0))
                .andExpect(jsonPath("$.focoPorMateria.length()").value(0));
    }

    @Test
    void osIdsDaAnaNaoExistemParaOBeto() throws Exception {
        mvc.perform(get("/api/materias/{id}", materiaDaAna)).andExpect(status().isNotFound());
        mvc.perform(get("/api/tarefas/{id}", tarefaDaAna)).andExpect(status().isNotFound());
        mvc.perform(get("/api/baralhos/{id}", baralhoDaAna)).andExpect(status().isNotFound());
        mvc.perform(get("/api/baralhos/{id}/cartoes", baralhoDaAna)).andExpect(status().isNotFound());
        mvc.perform(get("/api/baralhos/{id}/estudo", baralhoDaAna)).andExpect(status().isNotFound());
    }

    @Test
    void betoNaoAlteraNemApagaNadaDaAna() throws Exception {
        json(put("/api/materias/" + materiaDaAna), "{\"nome\": \"Hackeado\", \"cor\": \"#000000\"}")
                .andExpect(status().isNotFound());
        json(put("/api/tarefas/" + tarefaDaAna), "{\"titulo\": \"Hackeado\"}").andExpect(status().isNotFound());
        mvc.perform(patch("/api/tarefas/{id}/concluir", tarefaDaAna)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/baralhos/{id}", baralhoDaAna)).andExpect(status().isNotFound());
        json(post("/api/cartoes/%d/revisao".formatted(cartaoDaAna)), "{\"resposta\": \"BOM\"}")
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/pomodoro/sessoes/{id}/interromper", sessaoDaAna)).andExpect(status().isNotFound());

        // e de volta como Ana: tudo intacto
        Autenticacao.logarComo(usuarios, "ana@teste.dev");
        mvc.perform(get("/api/materias/{id}", materiaDaAna)).andExpect(jsonPath("$.nome").value("Grafos"));
        mvc.perform(get("/api/tarefas/{id}", tarefaDaAna)).andExpect(jsonPath("$.concluida").value(false));
        mvc.perform(get("/api/pomodoro/sessoes/atual")).andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void betoNaoUsaAMateriaNemATarefaDaAnaNasDele() throws Exception {
        json(post("/api/tarefas"), "{\"titulo\": \"Minha\", \"materiaId\": %d}".formatted(materiaDaAna))
                .andExpect(status().isNotFound());
        json(post("/api/baralhos/%d/cartoes".formatted(baralhoDaAna)), "{\"frente\": \"x\", \"verso\": \"y\"}")
                .andExpect(status().isNotFound());
        json(post("/api/pomodoro/sessoes"), "{\"tipo\": \"FOCO\", \"tarefaId\": %d}".formatted(tarefaDaAna))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadaUmTemSeuProprioPomodoroENomesDeMateria() throws Exception {
        // a Ana está com uma sessão rodando; isso não impede o Beto
        json(post("/api/pomodoro/sessoes"), "{\"tipo\": \"FOCO\"}").andExpect(status().isCreated());
        // e o Beto também pode ter uma matéria chamada "Grafos"
        json(post("/api/materias"), "{\"nome\": \"Grafos\", \"cor\": \"#111111\"}").andExpect(status().isCreated());
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
            String corpo) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static long id(ResultActions resultado) throws Exception {
        String json = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
