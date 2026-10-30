package dev.guilherme.notelogic.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.TestcontainersConfiguration;

/** O fluxo de verdade: cadastro e login devolvem um JWT, e é ele que abre as rotas protegidas. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void limpa() {
        usuarios.deleteAll(); // on delete cascade leva junto os dados de cada um
    }

    @Test
    void cadastroDevolveUmTokenQueAbreAsRotasProtegidas() throws Exception {
        String token = token(cadastrar("Ana", "ana@teste.dev", "senha-forte-123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.nome").value("Ana")));

        mvc.perform(get("/api/auth/eu").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@teste.dev"));
    }

    @Test
    void cadastroJaCriaAsCategoriasDaAgenda() throws Exception {
        String token = token(cadastrar("Ana", "ana@teste.dev", "senha-forte-123"));

        mvc.perform(get("/api/agenda/categorias").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.nome == 'Prova')].destaque").value(true))
                .andExpect(jsonPath("$[?(@.nome == 'Treino')].destaque").value(false));
    }

    @Test
    void semTokenOuComTokenAdulteradoDa401() throws Exception {
        String token = token(cadastrar("Ana", "ana@teste.dev", "senha-forte-123"));

        mvc.perform(get("/api/tarefas")).andExpect(status().isUnauthorized());

        // troca um caractere da assinatura: o servidor percebe na hora
        String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        mvc.perform(get("/api/tarefas").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComSenhaCertaEErrada() throws Exception {
        cadastrar("Ana", "ana@teste.dev", "senha-forte-123");

        entrar("ANA@teste.dev", "senha-forte-123") // e-mail não diferencia maiúsculas
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        entrar("ana@teste.dev", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));

        // e-mail que não existe: exatamente a mesma resposta
        entrar("ninguem@teste.dev", "senha-forte-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos"));
    }

    @Test
    void emailRepetidoDa409() throws Exception {
        cadastrar("Ana", "ana@teste.dev", "senha-forte-123");
        cadastrar("Outra Ana", "Ana@Teste.dev", "outra-senha-123").andExpect(status().isConflict());
    }

    @Test
    void validaOsCamposDoCadastro() throws Exception {
        cadastrar("Ana", "nao-e-email", "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void aSenhaFicaGuardadaComoHashBcrypt() throws Exception {
        cadastrar("Ana", "ana@teste.dev", "senha-forte-123");

        String hash = usuarios.findByEmailIgnoreCase("ana@teste.dev").orElseThrow().getSenhaHash();
        assertThat(hash).startsWith("$2").doesNotContain("senha-forte-123");
    }

    @Test
    void oPrimeiroUsuarioHerdaOsDadosDeAntesDoLogin() throws Exception {
        // uma matéria "antiga", sem dono, como as que existiam antes da V6
        jdbc.sql("insert into materia (nome, cor, criada_em) values ('Antiga', '#543EFE', :agora)")
                .param("agora", OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                .update();

        String primeiro = token(cadastrar("Ana", "ana@teste.dev", "senha-forte-123"));
        String segundo = token(cadastrar("Beto", "beto@teste.dev", "senha-forte-123"));

        mvc.perform(get("/api/materias").header(HttpHeaders.AUTHORIZATION, "Bearer " + primeiro))
                .andExpect(jsonPath("$[0].nome").value("Antiga"));
        mvc.perform(get("/api/materias").header(HttpHeaders.AUTHORIZATION, "Bearer " + segundo))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ResultActions cadastrar(String nome, String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"%s\", \"email\": \"%s\", \"senha\": \"%s\"}".formatted(nome, email, senha)));
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha)));
    }

    private static String token(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.token");
    }
}
