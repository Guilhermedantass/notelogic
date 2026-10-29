package dev.guilherme.notelogic;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;

import dev.guilherme.notelogic.usuario.Usuario;
import dev.guilherme.notelogic.usuario.UsuarioRepository;

/**
 * Para os testes que não são sobre login: põe um usuário no contexto de segurança do teste, e o MockMvc
 * (configurado com o Spring Security pelo @AutoConfigureMockMvc) envia as requisições como ele.
 * O fluxo real com token JWT é testado em AuthControllerTests.
 */
public final class Autenticacao {

    private Autenticacao() {
    }

    /** Cria o usuário se ainda não existir e "loga" com ele. Devolve o id, para criar dados em nome dele. */
    public static Long logarComo(UsuarioRepository usuarios, String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseGet(() -> usuarios.save(new Usuario("Teste", email, "hash-de-teste")));
        // o "nome" da autenticação é o id: o mesmo que o subject do JWT em produção (ver UsuarioAtual)
        TestSecurityContextHolder.setAuthentication(
                new TestingAuthenticationToken(usuario.getId().toString(), null, "ROLE_USER"));
        return usuario.getId();
    }
}
