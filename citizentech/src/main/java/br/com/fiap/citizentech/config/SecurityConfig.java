package br.com.fiap.citizentech.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import br.com.fiap.citizentech.security.JwtProperties;
import br.com.fiap.citizentech.security.SecurityErrorHandler;
import br.com.fiap.citizentech.security.TokenService;

@Configuration
public class SecurityConfig {

	private static final String ROLE_PREFIX = "ROLE_";
	private static final String GESTOR = "GESTOR";
	private static final String CUIDADOR = "CUIDADOR";
	private static final String MOTORISTA = "MOTORISTA";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler securityErrorHandler,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		return http

				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/registro").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/info", "/error").permitAll()

						.requestMatchers("/api/usuarios/**").hasRole(GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/paradas/**", "/api/linhas/**").authenticated()
						.requestMatchers("/api/paradas/**", "/api/linhas/**", "/api/totens/**").hasRole(GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/veiculos/**").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers("/api/veiculos/**").hasRole(GESTOR)

						.requestMatchers("/api/passageiros/**", "/api/cartoes/**", "/api/rotas-frequentes/**")
						.hasAnyRole(CUIDADOR, GESTOR)

						.requestMatchers(HttpMethod.GET, "/api/solicitacoes/**").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers(HttpMethod.PATCH, "/api/solicitacoes/*/status").hasAnyRole(MOTORISTA, GESTOR)
						.requestMatchers("/api/solicitacoes/**").hasRole(GESTOR)

						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(securityErrorHandler)
						.accessDeniedHandler(securityErrorHandler))
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${citizentech.cors.allowed-origins:*}") List<String> origens) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(origens);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setExposedHeaders(List.of("Location"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
		papeis.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
		papeis.setAuthorityPrefix(ROLE_PREFIX);

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(papeis);
		return converter;
	}

	@Bean
	public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(jwtProperties)));
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(jwtProperties))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
		return decoder;
	}

	private SecretKey chave(JwtProperties jwtProperties) {
		return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
