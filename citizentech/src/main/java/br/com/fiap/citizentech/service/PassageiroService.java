package br.com.fiap.citizentech.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.PassageiroRequest;
import br.com.fiap.citizentech.dto.response.PassageiroResponse;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.enums.Perfil;
import br.com.fiap.citizentech.repository.PassageiroRepository;
import br.com.fiap.citizentech.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassageiroService {

	private final PassageiroRepository passageiroRepository;
	private final UsuarioService usuarioService;

	public Page<PassageiroResponse> listar(Long cuidadorId, Pageable pageable, UsuarioLogado usuario) {
		Long filtro = usuario.isCuidador() ? usuario.id() : cuidadorId;
		Page<Passageiro> pagina = filtro != null
				? passageiroRepository.findByCuidadorId(filtro, pageable)
				: passageiroRepository.findAll(pageable);
		return pagina.map(PassageiroResponse::from);
	}

	public PassageiroResponse buscarPorId(Long id, UsuarioLogado usuario) {
		return PassageiroResponse.from(buscarComAcesso(id, usuario));
	}

	@Transactional
	public PassageiroResponse criar(PassageiroRequest request, UsuarioLogado usuario) {
		verificarCuidadorDoRequest(request, usuario);
		Passageiro passageiro = new Passageiro();
		preencher(passageiro, request);
		passageiro.setConsentimentoEm(agora());
		return PassageiroResponse.from(passageiroRepository.save(passageiro));
	}

	@Transactional
	public PassageiroResponse atualizar(Long id, PassageiroRequest request, UsuarioLogado usuario) {
		Passageiro passageiro = buscarComAcesso(id, usuario);
		verificarCuidadorDoRequest(request, usuario);
		boolean novaVersaoDoTermo = !passageiro.getVersaoTermo().equals(request.versaoTermo().trim());
		preencher(passageiro, request);
		// LGPD: nova versao do termo registra um novo consentimento
		if (novaVersaoDoTermo) {
			passageiro.setConsentimentoEm(agora());
		}
		return PassageiroResponse.from(passageiro);
	}

	@Transactional
	public void excluir(Long id, UsuarioLogado usuario) {
		passageiroRepository.delete(buscarComAcesso(id, usuario));
	}

	public Passageiro buscarEntidade(Long id) {
		return passageiroRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Passageiro", id));
	}

	public Passageiro buscarComAcesso(Long id, UsuarioLogado usuario) {
		Passageiro passageiro = buscarEntidade(id);
		verificarAcesso(passageiro, usuario);
		return passageiro;
	}

	public void verificarAcesso(Passageiro passageiro, UsuarioLogado usuario) {
		if (usuario.isCuidador() && !passageiro.getCuidador().getId().equals(usuario.id())) {
			throw new AccessDeniedException("Este passageiro pertence a outro cuidador");
		}
	}

	private void verificarCuidadorDoRequest(PassageiroRequest request, UsuarioLogado usuario) {
		if (usuario.isCuidador() && !request.cuidadorId().equals(usuario.id())) {
			throw new AccessDeniedException("O cuidador so pode cadastrar passageiros para si mesmo");
		}
	}

	private void preencher(Passageiro passageiro, PassageiroRequest request) {
		passageiro.setCuidador(usuarioService.buscarComPerfil(request.cuidadorId(), Perfil.CUIDADOR));
		passageiro.setNome(request.nome().trim());
		passageiro.setTipoNecessidade(request.tipoNecessidade());
		passageiro.setContatoEmergencia(request.contatoEmergencia().trim());
		passageiro.setVersaoTermo(request.versaoTermo().trim());
	}


	// Mesma precisao do banco (microssegundos)
	private static LocalDateTime agora() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
	}

}
