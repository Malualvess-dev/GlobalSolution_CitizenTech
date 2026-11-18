package br.com.fiap.citizentech.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.fiap.citizentech.model.Usuario;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

	public static final String CLAIM_PERFIL = "perfil";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public TokenGerado gerar(Usuario usuario) {
		Instant agora = Instant.now();
		Instant expiraEm = agora.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.subject(usuario.getId().toString())
				.issuedAt(agora)
				.expiresAt(expiraEm)
				.claim(CLAIM_PERFIL, usuario.getPerfil().name())
				.claim("nome", usuario.getNome())
				.claim("email", usuario.getEmail())
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenGerado(token, expiraEm);
	}

	public record TokenGerado(String valor, Instant expiraEm) {
	}

}
