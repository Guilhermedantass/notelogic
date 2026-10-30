package dev.guilherme.notelogic.agenda;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import dev.guilherme.notelogic.Autenticacao;
import dev.guilherme.notelogic.TestcontainersConfiguration;
import dev.guilherme.notelogic.materia.Materia;
import dev.guilherme.notelogic.materia.MateriaRepository;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AgendaControllerTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EventoRepository eventos;

    @Autowired
    CategoriaEventoRepository categorias;

    @Autowired
    MateriaRepository materias;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    JdbcClient jdbc;

    Long eu;

    @BeforeEach
    void limpa() {
        eventos.deleteAll();
        categorias.deleteAll();
        eu = Autenticacao.logarComo(usuarios, "eu@teste.dev");
    }

    // --- categorias ---

    @Test
    void categoriaComNomeRepetidoDa409MesmoComOutraCaixa() throws Exception {
        criarCategoria("Treino", "#22A06B", false);

        mvc.perform(post("/api/agenda/categorias").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome": "TREINO", "cor": "#000000"}
                """))
                .andExpect(status().isConflict());
    }

    @Test
    void apagarCategoriaDeixaOEventoSemCategoria() throws Exception {
        Long categoria = criarCategoria("Prova", "#E5484D", true);
        Long evento = id(criarEvento("""
                {"titulo": "Prova de Grafos", "categoriaId": %d, "dataInicio": "2026-11-20", "diaInteiro": true}
                """.formatted(categoria)));

        mvc.perform(delete("/api/agenda/categorias/{id}", categoria)).andExpect(status().isNoContent());

        mvc.perform(get("/api/eventos/{id}", evento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").doesNotExist())
                .andExpect(jsonPath("$.cor").value(AgendaDtos.COR_PADRAO));
    }

    // --- eventos ---

    @Test
    void eventoDeUmDiaComHorarioETodosOsCampos() throws Exception {
        Long categoria = criarCategoria("Prova", "#E5484D", true);
        Long materia = materias.save(new Materia(eu, "Grafos", "#543EFE")).getId();

        criarEvento("""
                {"titulo": "Prova de Grafos", "descricao": "Caps. 1 a 4", "local": "Sala 204",
                 "categoriaId": %d, "materiaId": %d, "dataInicio": "2026-11-20", "horaInicio": "14:00"}
                """.formatted(categoria, materia))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataFim").value("2026-11-20")) // sem dataFim: evento de um dia
                .andExpect(jsonPath("$.diaInteiro").value(false))
                .andExpect(jsonPath("$.horaInicio").value("14:00:00"))
                .andExpect(jsonPath("$.horaFim").doesNotExist()) // hora de fim é opcional
                .andExpect(jsonPath("$.local").value("Sala 204"))
                .andExpect(jsonPath("$.categoria.destaque").value(true))
                .andExpect(jsonPath("$.materia.nome").value("Grafos"))
                .andExpect(jsonPath("$.cor").value("#E5484D")); // a categoria vence a matéria
    }

    @Test
    void semCategoriaACorVemDaMateria() throws Exception {
        Long materia = materias.save(new Materia(eu, "Redes", "#0EA5E9")).getId();

        criarEvento("""
                {"titulo": "Aula de Redes", "materiaId": %d, "dataInicio": "2026-11-03", "horaInicio": "19:00",
                 "horaFim": "21:00"}
                """.formatted(materia))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cor").value("#0EA5E9"));
    }

    @Test
    void diaInteiroDescartaAsHoras() throws Exception {
        criarEvento("""
                {"titulo": "Feriado", "dataInicio": "2026-11-15", "diaInteiro": true, "horaInicio": "10:00",
                 "horaFim": "11:00"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").doesNotExist())
                .andExpect(jsonPath("$.horaFim").doesNotExist());
    }

    @Test
    void eventoDeVariosDiasPodeTerminarMaisCedoNoRelogio() throws Exception {
        // viagem: sai sexta às 20h e volta domingo às 18h
        criarEvento("""
                {"titulo": "Viagem", "dataInicio": "2026-11-06", "dataFim": "2026-11-08", "horaInicio": "20:00",
                 "horaFim": "18:00"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataFim").value("2026-11-08"));
    }

    @Test
    void periodoIncoerenteDa422() throws Exception {
        criarEvento("""
                {"titulo": "X", "dataInicio": "2026-11-10", "dataFim": "2026-11-09", "diaInteiro": true}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("O fim não pode ser antes do início"));

        criarEvento("""
                {"titulo": "X", "dataInicio": "2026-11-10"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                        .value("Informe a hora de início ou marque o evento como dia inteiro"));

        criarEvento("""
                {"titulo": "X", "dataInicio": "2026-11-10", "horaInicio": "14:00", "horaFim": "13:00"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A hora de fim precisa ser depois da hora de início"));
    }

    @Test
    void semTituloOuSemDataDa400() throws Exception {
        criarEvento("""
                {"titulo": " ", "diaInteiro": true}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo").exists())
                .andExpect(jsonPath("$.erros.dataInicio").exists());
    }

    @Test
    void oBancoTambemBarraUmPeriodoIncoerente() {
        // mesmo passando por fora do PeriodoEvento, o check da V8 segura
        assertThatThrownBy(() -> jdbc.sql("""
                insert into evento (usuario_id, titulo, data_inicio, data_fim, dia_inteiro, hora_inicio, criado_em)
                values (:u, 'X', '2026-11-10', '2026-11-10', true, '10:00', now())
                """).param("u", eu).update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("evento_dia_inteiro_sem_hora");
    }

    @Test
    void listaOQueTocaOPeriodoEmOrdem() throws Exception {
        // semana de 02 a 08/11/2026
        criarEvento("""
                {"titulo": "Semana de provas", "dataInicio": "2026-10-30", "dataFim": "2026-11-03", "diaInteiro": true}
                """); // começou antes, mas ainda está acontecendo
        criarEvento("""
                {"titulo": "Aula", "dataInicio": "2026-11-03", "horaInicio": "19:00"}
                """);
        criarEvento("""
                {"titulo": "Treino", "dataInicio": "2026-11-03", "horaInicio": "09:00"}
                """);
        criarEvento("""
                {"titulo": "Mês que vem", "dataInicio": "2026-12-01", "diaInteiro": true}
                """);

        mvc.perform(get("/api/eventos").param("de", "2026-11-02").param("ate", "2026-11-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].titulo").value("Semana de provas"))
                .andExpect(jsonPath("$[1].titulo").value("Treino"))
                .andExpect(jsonPath("$[2].titulo").value("Aula"));
    }

    @Test
    void periodoDeConsultaInvalidoDa422() throws Exception {
        mvc.perform(get("/api/eventos").param("de", "2026-11-08").param("ate", "2026-11-02"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/eventos").param("de", "2026-01-01").param("ate", "2027-06-01"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void editaEApaga() throws Exception {
        Long id = id(criarEvento("""
                {"titulo": "Treino", "dataInicio": "2026-11-03", "horaInicio": "09:00"}
                """));

        mvc.perform(put("/api/eventos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo": "Treino de perna", "local": "Academia", "dataInicio": "2026-11-03", "horaInicio": "18:00",
                 "horaFim": "19:00"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Treino de perna"))
                .andExpect(jsonPath("$.horaInicio").value("18:00:00"));

        mvc.perform(delete("/api/eventos/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/eventos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void naoVeNemUsaDadosDeOutraPessoa() throws Exception {
        Long outro = Autenticacao.logarComo(usuarios, "outro@teste.dev");
        Long categoriaDoOutro = categorias.save(new CategoriaEvento(outro, "Dele", "#000000", false)).getId();
        Long eventoDoOutro = eventos.save(new Evento(outro, "Dele", null, null, null, null,
                PeriodoEvento.de(LocalDate.of(2026, 11, 3), null, false, LocalTime.of(9, 0), null))).getId();

        Autenticacao.logarComo(usuarios, "eu@teste.dev");

        mvc.perform(get("/api/eventos/{id}", eventoDoOutro)).andExpect(status().isNotFound());
        mvc.perform(get("/api/eventos").param("de", "2026-11-01").param("ate", "2026-11-30"))
                .andExpect(jsonPath("$.length()").value(0));
        criarEvento("""
                {"titulo": "X", "categoriaId": %d, "dataInicio": "2026-11-03", "diaInteiro": true}
                """.formatted(categoriaDoOutro))
                .andExpect(status().isNotFound());
    }

    private Long criarCategoria(String nome, String cor, boolean destaque) throws Exception {
        return id(mvc.perform(post("/api/agenda/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"%s\", \"cor\": \"%s\", \"destaque\": %s}".formatted(nome, cor, destaque)))
                .andExpect(status().isCreated()));
    }

    private ResultActions criarEvento(String json) throws Exception {
        return mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static Long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
