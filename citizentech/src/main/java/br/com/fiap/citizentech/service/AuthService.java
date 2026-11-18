package br.com.fiap.citizentech.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.LoginRequest;
import br.com.fiap.citizentech.dto.request.RegistroRequest;
import br.com.fiap.citizentech.dto.request.UsuarioRequest;
import br.com.fiap.citizentech.dto.response.LoginResponse;
import br.com.fiap.citizentech.dto.response.UsuarioResponse;
import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.UsuarioRepository;
import br.com.fiap.citizentech.security.TokenService;
import br.com.fiap.citizentech.security.TokenService.TokenGerado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha invalidos";

	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public LoginResponse login(LoginRequest request) {
		Usuario usuario = usuarioRepository.findByEmail(request.email().trim().toLowerCase())
				.orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));

		if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
			throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
		}

		TokenGerado token = tokenService.gerar(usuario);
		return new LoginResponse(token.valor(), "Bearer", token.expiraEm(), UsuarioResponse.from(usuario));
	}

	@Transactional
	public UsuarioResponse registrar(RegistroRequest request) {
		return usuarioService.criar(new UsuarioRequest(
				request.nome(),
				request.email(),
				request.senha(),
				Perfil.CUIDADOR,
				request.expoPushToken()));
	}

	public UsuarioResponse usuarioAtual(Long id) {
		return usuarioService.buscarPorId(id);
	}

}
