package dev.guilherme.notelogic.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.guilherme.notelogic.erro.ConflitoException;
import dev.guilherme.notelogic.erro.CredenciaisInvalidasException;
import dev.guilherme.notelogic.erro.RecursoNaoEncontradoException;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final AdocaoDeDadosAntigos adocao;
    private final UsuarioAtual atual;

    /** Hash de uma senha qualquer, usado quando o e-mail não existe (ver entrar). */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, TokenService tokens,
            AdocaoDeDadosAntigos adocao, UsuarioAtual atual) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.adocao = adocao;
        this.atual = atual;
        this.hashFalso = encoder.encode("senha-que-ninguem-usa");
    }

    @Transactional
    public AuthDtos.SessaoResponse cadastrar(String nome, String email, String senha) {
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe uma conta com este e-mail");
        }
        Usuario usuario = usuarios.save(new Usuario(nome.trim(), emailNormalizado, encoder.encode(senha)));
        if (usuarios.count() == 1) {
            adocao.adotar(usuario.getId());
        }
        return sessao(usuario);
    }

    @Transactional(readOnly = true)
    public AuthDtos.SessaoResponse entrar(String email, String senha) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        // Mesmo sem usuário, roda o BCrypt: assim "e-mail não existe" e "senha errada" demoram o mesmo tempo,
        // e ninguém descobre quais e-mails têm conta medindo o tempo de resposta.
        boolean senhaConfere = encoder.matches(senha, usuario != null ? usuario.getSenhaHash() : hashFalso);
        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException(); // mesma mensagem nos dois casos, pelo mesmo motivo
        }
        return sessao(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario eu() {
        Long id = atual.id();
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private AuthDtos.SessaoResponse sessao(Usuario usuario) {
        return new AuthDtos.SessaoResponse(tokens.gerar(usuario), AuthDtos.UsuarioResponse.de(usuario));
    }
}
