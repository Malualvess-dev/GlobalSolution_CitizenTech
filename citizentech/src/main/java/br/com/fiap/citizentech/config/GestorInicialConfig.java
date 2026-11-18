package br.com.fiap.citizentech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.citizentech.dto.request.UsuarioRequest;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.UsuarioRepository;
import br.com.fiap.citizentech.service.UsuarioService;

@Configuration
public class GestorInicialConfig {

	private static final Logger log = LoggerFactory.getLogger(GestorInicialConfig.class);

	@Bean
	public ApplicationRunner criarGestorInicial(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
			@Value("${citizentech.gestor-inicial.nome:Gestor}") String nome,
			@Value("${citizentech.gestor-inicial.email:}") String email,
			@Value("${citizentech.gestor-inicial.senha:}") String senha) {
		return args -> {
			if (email.isBlank() || senha.isBlank()) {
				return;
			}
			if (usuarioRepository.existsByEmail(email.trim().toLowerCase())) {
				log.info("Gestor inicial {} ja existe", email);
				return;
			}
			usuarioService.criar(new UsuarioRequest(nome, email, senha, Perfil.GESTOR, null));
			log.info("Gestor inicial {} criado", email);
		};
	}

}
