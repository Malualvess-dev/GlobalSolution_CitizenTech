package br.com.fiap.citizentech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.dto.request.TotemRequest;
import br.com.fiap.citizentech.dto.response.TotemResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.exception.RegraNegocioException;
import br.com.fiap.citizentech.model.Totem;
import br.com.fiap.citizentech.model.enums.StatusTotem;
import br.com.fiap.citizentech.repository.TotemRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TotemService {

	private final TotemRepository totemRepository;
	private final ParadaService paradaService;
	private final HashService hashService;

	public Page<TotemResponse> listar(Pageable pageable) {
		return totemRepository.findAll(pageable).map(TotemResponse::from);
	}

	public TotemResponse buscarPorId(Long id) {
		return TotemResponse.from(buscarEntidade(id));
	}

	@Transactional
	public TotemResponse criar(TotemRequest request) {
		String identificador = request.identificador().trim();
		if (totemRepository.existsByIdentificador(identificador)) {
			throw new ConflitoException("Ja existe um totem com o identificador " + identificador);
		}
		if (request.token() == null || request.token().isBlank()) {
			throw new RegraNegocioException("O token do dispositivo e obrigatorio no cadastro do totem");
		}
		Totem totem = new Totem();
		preencher(totem, request, identificador);
		return TotemResponse.from(totemRepository.save(totem));
	}

	@Transactional
	public TotemResponse atualizar(Long id, TotemRequest request) {
		Totem totem = buscarEntidade(id);
		String identificador = request.identificador().trim();
		if (totemRepository.existsByIdentificadorAndIdNot(identificador, id)) {
			throw new ConflitoException("Ja existe um totem com o identificador " + identificador);
		}
		preencher(totem, request, identificador);
		return TotemResponse.from(totem);
	}

	@Transactional
	public void excluir(Long id) {
		totemRepository.delete(buscarEntidade(id));
	}

	public Totem buscarEntidade(Long id) {
		return totemRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Totem", id));
	}

	private void preencher(Totem totem, TotemRequest request, String identificador) {
		totem.setParada(paradaService.buscarEntidade(request.paradaId()));
		totem.setIdentificador(identificador);
		totem.setStatus(request.status() != null ? request.status() : StatusTotem.ATIVO);

		if (request.token() != null && !request.token().isBlank()) {
			totem.setTokenHash(hashService.hmacSha256(request.token()));
		}
	}

}
