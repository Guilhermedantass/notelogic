package dev.guilherme.notelogic.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem está fazendo a requisição. O Spring Security valida o token e guarda o resultado no
 * SecurityContext (um por thread, ou seja, um por requisição); aqui só lemos de lá.
 *
 * O "nome" da autenticação é o subject do JWT, que é o id do usuário (ver TokenService).
 */
@Component
public class UsuarioAtual {

    public Long id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado nesta requisição");
        }
        return Long.valueOf(autenticacao.getName());
    }
}
