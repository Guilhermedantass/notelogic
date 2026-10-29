package dev.guilherme.notelogic.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.guilherme.notelogic.usuario.AuthDtos.CadastroRequest;
import dev.guilherme.notelogic.usuario.AuthDtos.LoginRequest;
import dev.guilherme.notelogic.usuario.AuthDtos.SessaoResponse;
import dev.guilherme.notelogic.usuario.AuthDtos.UsuarioResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** Público (liberado em Seguranca). Já devolve o token: quem se cadastra entra direto. */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse cadastrar(@Valid @RequestBody CadastroRequest request) {
        return service.cadastrar(request.nome(), request.email(), request.senha());
    }

    /** Público. */
    @PostMapping("/login")
    public SessaoResponse entrar(@Valid @RequestBody LoginRequest request) {
        return service.entrar(request.email(), request.senha());
    }

    /** Protegido: só responde com um token válido. O front usa para saber se o token ainda vale. */
    @GetMapping("/eu")
    public UsuarioResponse eu() {
        return UsuarioResponse.de(service.eu());
    }
}
