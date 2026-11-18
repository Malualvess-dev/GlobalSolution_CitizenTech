package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.UsuarioRequest;
import br.com.fiap.citizentech.dto.response.UsuarioResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.exception.RegraNegocioException;
import br.com.fiap.citizentech.model.Usuario;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public Page<UsuarioResponse> listar(Pageable pageable) {
		return usuarioRepository.findAll(pageable).map(UsuarioResponse::from);
	}

	public UsuarioResponse buscarPorId(Long id) {
		return UsuarioResponse.from(buscarEntidade(id));
	}

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		String email = normalizarEmail(request.email());
		if (usuarioRepository.existsByEmail(email)) {
			throw new ConflitoException("Ja existe um usuario com o e-mail " + email);
		}
		Usuario usuario = new Usuario();
		preencher(usuario, request, email);
		return UsuarioResponse.from(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
		Usuario usuario = buscarEntidade(id);
		String email = normalizarEmail(request.email());
		if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
			throw new ConflitoException("Ja existe um usuario com o e-mail " + email);
		}
		preencher(usuario, request, email);
		return UsuarioResponse.from(usuario);
	}

	@Transactional
	public void excluir(Long id) {
		usuarioRepository.delete(buscarEntidade(id));
	}

	public Usuario buscarEntidade(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", id));
	}

	public Usuario buscarComPerfil(Long id, Perfil perfilEsperado) {
		Usuario usuario = buscarEntidade(id);
		if (usuario.getPerfil() != perfilEsperado) {
			throw new RegraNegocioException(
					"O usuario " + id + " precisa ter o perfil " + perfilEsperado + ", mas e " + usuario.getPerfil());
		}
		return usuario;
	}

	private void preencher(Usuario usuario, UsuarioRequest request, String email) {
		usuario.setNome(request.nome().trim());
		usuario.setEmail(email);
		usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
		usuario.setPerfil(request.perfil());
		usuario.setExpoPushToken(request.expoPushToken());
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}

}
