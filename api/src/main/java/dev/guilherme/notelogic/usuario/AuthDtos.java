package dev.guilherme.notelogic.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** O BCrypt só considera os primeiros 72 bytes da senha: daí o máximo. */
    public record CadastroRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email) {

        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail());
        }
    }

    public record SessaoResponse(String token, UsuarioResponse usuario) {
    }
}
