package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.RotaFrequenteRequest;
import br.com.fiap.citizentech.dto.response.RotaFrequenteResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.model.RotaFrequente;
import br.com.fiap.citizentech.repository.RotaFrequenteRepository;
import br.com.fiap.citizentech.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RotaFrequenteService {

	private static final String MSG_DUPLICADA = "O passageiro ja tem uma rota frequente com essa parada e essa linha";

	private final RotaFrequenteRepository rotaFrequenteRepository;
	private final PassageiroService passageiroService;
	private final ParadaService paradaService;
	private final LinhaService linhaService;

	public Page<RotaFrequenteResponse> listar(Long passageiroId, Pageable pageable, UsuarioLogado usuario) {
		Page<RotaFrequente> pagina;
		if (passageiroId != null) {
			passageiroService.buscarComAcesso(passageiroId, usuario);
			pagina = rotaFrequenteRepository.findByPassageiroId(passageiroId, pageable);
		} else if (usuario.isCuidador()) {
			pagina = rotaFrequenteRepository.findByPassageiroCuidadorId(usuario.id(), pageable);
		} else {
			pagina = rotaFrequenteRepository.findAll(pageable);
		}
		return pagina.map(RotaFrequenteResponse::from);
	}

	public RotaFrequenteResponse buscarPorId(Long id, UsuarioLogado usuario) {
		return RotaFrequenteResponse.from(buscarComAcesso(id, usuario));
	}

	@Transactional
	public RotaFrequenteResponse criar(RotaFrequenteRequest request, UsuarioLogado usuario) {
		if (rotaFrequenteRepository.existsByPassageiroIdAndParadaIdAndLinhaId(
				request.passageiroId(), request.paradaId(), request.linhaId())) {
			throw new ConflitoException(MSG_DUPLICADA);
		}
		RotaFrequente rota = new RotaFrequente();
		preencher(rota, request, usuario);
		return RotaFrequenteResponse.from(rotaFrequenteRepository.save(rota));
	}

	@Transactional
	public RotaFrequenteResponse atualizar(Long id, RotaFrequenteRequest request, UsuarioLogado usuario) {
		RotaFrequente rota = buscarComAcesso(id, usuario);
		if (rotaFrequenteRepository.existsByPassageiroIdAndParadaIdAndLinhaIdAndIdNot(
				request.passageiroId(), request.paradaId(), request.linhaId(), id)) {
			throw new ConflitoException(MSG_DUPLICADA);
		}
		preencher(rota, request, usuario);
		return RotaFrequenteResponse.from(rota);
	}

	@Transactional
	public void excluir(Long id, UsuarioLogado usuario) {
		rotaFrequenteRepository.delete(buscarComAcesso(id, usuario));
	}

	public RotaFrequente buscarEntidade(Long id) {
		return rotaFrequenteRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Rota frequente", id));
	}

	private RotaFrequente buscarComAcesso(Long id, UsuarioLogado usuario) {
		RotaFrequente rota = buscarEntidade(id);
		passageiroService.verificarAcesso(rota.getPassageiro(), usuario);
		return rota;
	}

	private void preencher(RotaFrequente rota, RotaFrequenteRequest request, UsuarioLogado usuario) {
		rota.setPassageiro(passageiroService.buscarComAcesso(request.passageiroId(), usuario));
		rota.setParada(paradaService.buscarEntidade(request.paradaId()));
		rota.setLinha(linhaService.buscarEntidade(request.linhaId()));
		rota.setApelido(request.apelido());
	}

}
