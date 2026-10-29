package dev.guilherme.notelogic.usuario;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Os dados criados antes do login existir (V6) ficaram sem dono. O primeiro usuário que se cadastra
 * herda todos eles: na prática, é você, que já vinha usando o app.
 */
@Component
public class AdocaoDeDadosAntigos {

    private static final String[] TABELAS = { "materia", "tarefa", "baralho", "sessao_foco" };

    private final JdbcClient jdbc;

    public AdocaoDeDadosAntigos(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Roda dentro da transação do cadastro: ou adota tudo, ou nada. */
    public int adotar(Long usuarioId) {
        int total = 0;
        for (String tabela : TABELAS) {
            // nome da tabela vem da constante acima, nunca de entrada do usuário
            total += jdbc.sql("update " + tabela + " set usuario_id = :id where usuario_id is null")
                    .param("id", usuarioId)
                    .update();
        }
        return total;
    }
}
