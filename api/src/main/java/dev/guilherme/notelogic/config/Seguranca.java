package dev.guilherme.notelogic.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de segurança da API.
 *
 * Toda requisição passa por uma cadeia de filtros ANTES de chegar no controller. O filtro do
 * "resource server" procura o cabeçalho Authorization: Bearer <token>, confere a assinatura e a validade
 * do JWT e, se estiver tudo certo, registra o usuário no SecurityContext. Sem token válido, as rotas
 * protegidas respondem 401 e o controller nem é chamado.
 */
@Configuration
public class Seguranca {

    @Bean
    SecurityFilterChain cadeiaDeFiltros(HttpSecurity http) throws Exception {
        return http
                // CSRF protege sessões baseadas em cookie. Aqui o token vai num cabeçalho que o navegador
                // não envia sozinho, então esse ataque não se aplica.
                .csrf(csrf -> csrf.disable())
                // Nada de sessão no servidor: cada requisição se prova sozinha com o token
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(HttpMethod.POST, "/api/auth/cadastro", "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
                .build();
    }

    /** BCrypt: lento de propósito e com sal aleatório, para que vazar o banco não entregue as senhas. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey chaveJwt(@Value("${notelogic.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("notelogic.jwt.segredo precisa ter pelo menos 32 bytes para HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Assina tokens (usado no login). */
    @Bean
    JwtEncoder jwtEncoder(SecretKey chave) {
        return NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build();
    }

    /** Confere tokens (usado em toda requisição protegida): assinatura, expiração e formato. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
