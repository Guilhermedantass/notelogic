package dev.guilherme.notelogic.usuario;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Gera o JWT entregue no login. Um JWT tem três partes separadas por ponto: cabeçalho, conteúdo
 * (as "claims") e assinatura. Qualquer um consegue LER o conteúdo (é só base64), mas ninguém consegue
 * ALTERAR sem invalidar a assinatura, que só quem tem o segredo produz. Por isso: nada de senha no token.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration validade;

    public TokenService(JwtEncoder encoder, @Value("${notelogic.jwt.validade}") Duration validade) {
        this.encoder = encoder;
        this.validade = validade;
    }

    public String gerar(Usuario usuario) {
        // Relógio real de propósito (e não o Clock do app): validade de token é segurança, não regra de negócio
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("notelogic")
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .claim("nome", usuario.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
