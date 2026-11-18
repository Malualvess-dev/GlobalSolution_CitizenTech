package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.CartaoRequest;
import br.com.fiap.citizentech.dto.response.CartaoResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.Cartao;
import br.com.fiap.citizentech.repository.CartaoRepository;
import br.com.fiap.citizentech.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartaoService {

	private final CartaoRepository cartaoRepository;
	private final PassageiroService passageiroService;
	private final HashService hashService;

	public Page<CartaoResponse> listar(Long passageiroId, Pageable pageable, UsuarioLogado usuario) {
		Page<Cartao> pagina;
		if (passageiroId != null) {
			passageiroService.buscarComAcesso(passageiroId, usuario);
			pagina = cartaoRepository.findByPassageiroId(passageiroId, pageable);
		} else if (usuario.isCuidador()) {
			pagina = cartaoRepository.findByPassageiroCuidadorId(usuario.id(), pageable);
		} else {
			pagina = cartaoRepository.findAll(pageable);
		}
		return pagina.map(CartaoResponse::from);
	}

	public CartaoResponse buscarPorId(Long id, UsuarioLogado usuario) {
		return CartaoResponse.from(buscarComAcesso(id, usuario));
	}

	@Transactional
	public CartaoResponse criar(CartaoRequest request, UsuarioLogado usuario) {
		String uidHash = hashService.hmacSha256(request.uid());
		if (cartaoRepository.existsByUidHash(uidHash)) {
			throw new ConflitoException("Este cartao ja esta vinculado a um passageiro");
		}
		Cartao cartao = new Cartao();
		preencher(cartao, request, uidHash, usuario);
		return CartaoResponse.from(cartaoRepository.save(cartao));
	}

	@Transactional
	public CartaoResponse atualizar(Long id, CartaoRequest request, UsuarioLogado usuario) {
		Cartao cartao = buscarComAcesso(id, usuario);
		String uidHash = hashService.hmacSha256(request.uid());
		if (cartaoRepository.existsByUidHashAndIdNot(uidHash, id)) {
			throw new ConflitoException("Este cartao ja esta vinculado a um passageiro");
		}
		preencher(cartao, request, uidHash, usuario);
		return CartaoResponse.from(cartao);
	}

	@Transactional
	public void excluir(Long id, UsuarioLogado usuario) {
		cartaoRepository.delete(buscarComAcesso(id, usuario));
	}

	public Cartao buscarEntidade(Long id) {
		return cartaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Cartao", id));
	}

	private Cartao buscarComAcesso(Long id, UsuarioLogado usuario) {
		Cartao cartao = buscarEntidade(id);
		passageiroService.verificarAcesso(cartao.getPassageiro(), usuario);
		return cartao;
	}

	private void preencher(Cartao cartao, CartaoRequest request, String uidHash, UsuarioLogado usuario) {
		cartao.setPassageiro(passageiroService.buscarComAcesso(request.passageiroId(), usuario));
		cartao.setUidHash(uidHash);
		cartao.setAtivo(request.ativo() == null || request.ativo());
	}

}
