package br.com.fiap.citizentech.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.ai.ContextoMensagemVoz;
import br.com.fiap.citizentech.ai.MensagemVozService;
import br.com.fiap.citizentech.dto.request.SolicitacaoEmbarqueRequest;
import br.com.fiap.citizentech.dto.request.StatusSolicitacaoRequest;
import br.com.fiap.citizentech.dto.response.AuditoriaSolicitacaoResponse;
import br.com.fiap.citizentech.dto.response.SolicitacaoEmbarqueResponse;
import br.com.fiap.citizentech.dto.response.TentativaEmbarqueResponse;
import br.com.fiap.citizentech.exception.ConflitoException;
import br.com.fiap.citizentech.exception.RecursoNaoEncontradoException;
import br.com.fiap.citizentech.exception.RegraNegocioException;
import br.com.fiap.citizentech.messaging.SolicitacaoCriadaEvent;
import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.Passageiro;
import br.com.fiap.citizentech.model.SolicitacaoEmbarque;
import br.com.fiap.citizentech.model.Totem;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.StatusTotem;
import br.com.fiap.citizentech.repository.AuditoriaSolicitacaoRepository;
import br.com.fiap.citizentech.repository.SolicitacaoEmbarqueRepository;
import br.com.fiap.citizentech.repository.TentativaEmbarqueRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolicitacaoEmbarqueService {

	private final SolicitacaoEmbarqueRepository solicitacaoRepository;
	private final TentativaEmbarqueRepository tentativaRepository;
	private final AuditoriaSolicitacaoRepository auditoriaRepository;
	private final TotemService totemService;
	private final PassageiroService passageiroService;
	private final LinhaService linhaService;
	private final MensagemVozService mensagemVozService;
	private final ApplicationEventPublisher eventPublisher;

	public Page<SolicitacaoEmbarqueResponse> listar(StatusSolicitacao status, Pageable pageable) {
		Page<SolicitacaoEmbarque> pagina = status != null
				? solicitacaoRepository.findByStatus(status, pageable)
				: solicitacaoRepository.findAll(pageable);
		return pagina.map(SolicitacaoEmbarqueResponse::from);
	}

	public SolicitacaoEmbarqueResponse buscarPorId(Long id) {
		return SolicitacaoEmbarqueResponse.from(buscarEntidade(id));
	}

	@Transactional
	public SolicitacaoEmbarqueResponse criar(SolicitacaoEmbarqueRequest request) {
		String chave = request.chaveIdempotencia().trim();
		if (solicitacaoRepository.existsByChaveIdempotencia(chave)) {
			throw new ConflitoException("Ja existe uma solicitacao com a chave de idempotencia " + chave);
		}

		Totem totem = totemService.buscarEntidade(request.totemId());
		if (totem.getStatus() != StatusTotem.ATIVO) {
			throw new RegraNegocioException("O totem " + totem.getIdentificador() + " esta inativo");
		}

		SolicitacaoEmbarque solicitacao = new SolicitacaoEmbarque();
		solicitacao.setTotem(totem);
		solicitacao.setTipoSinal(request.tipoSinal());
		solicitacao.setChaveIdempotencia(chave);
		solicitacao.setMensagemVoz(request.mensagemVoz());
		solicitacao.setStatus(StatusSolicitacao.CRIADA);

		if (request.passageiroId() != null) {
			Passageiro passageiro = passageiroService.buscarEntidade(request.passageiroId());
			if (solicitacaoRepository.existsByPassageiroIdAndStatusIn(passageiro.getId(), StatusSolicitacao.ATIVOS)) {
				throw new ConflitoException("O passageiro " + passageiro.getId() + " ja tem uma solicitacao ativa");
			}
			solicitacao.setPassageiro(passageiro);
		}
		if (request.linhaId() != null) {
			solicitacao.setLinha(linhaService.buscarEntidade(request.linhaId()));
		}
		if (request.mensagemVoz() == null || request.mensagemVoz().isBlank()) {
			solicitacao.setMensagemVoz(mensagemVozService.gerar(contextoDaMensagem(solicitacao)));
		}

		SolicitacaoEmbarque salva = solicitacaoRepository.save(solicitacao);

		eventPublisher.publishEvent(new SolicitacaoCriadaEvent(
				salva.getId(),
				totem.getId(),
				salva.getLinha() != null ? salva.getLinha().getId() : null,
				salva.getTipoSinal(),
				salva.getCriadaEm()));
		return SolicitacaoEmbarqueResponse.from(salva);
	}

	@Transactional
	public SolicitacaoEmbarqueResponse atualizarStatus(Long id, StatusSolicitacaoRequest request) {
		SolicitacaoEmbarque solicitacao = buscarEntidade(id);
		StatusSolicitacao atual = solicitacao.getStatus();
		StatusSolicitacao novo = request.status();

		if (atual.isFinal()) {
			throw new RegraNegocioException("A solicitacao " + id + " ja foi encerrada como " + atual
					+ " e nao pode mais mudar de status");
		}
		if (novo == StatusSolicitacao.CRIADA) {
			throw new RegraNegocioException("Uma solicitacao nao pode voltar para o status CRIADA");
		}

		solicitacao.setStatus(novo);
		return SolicitacaoEmbarqueResponse.from(solicitacao);
	}

	@Transactional
	public void excluir(Long id) {
		solicitacaoRepository.delete(buscarEntidade(id));
	}

	public List<TentativaEmbarqueResponse> listarTentativas(Long solicitacaoId) {
		buscarEntidade(solicitacaoId);
		return tentativaRepository.findBySolicitacaoIdOrderByAlertadoEmAsc(solicitacaoId).stream()
				.map(TentativaEmbarqueResponse::from)
				.toList();
	}

	public List<AuditoriaSolicitacaoResponse> listarAuditoria(Long solicitacaoId) {
		return auditoriaRepository.findBySolicitacaoIdOrderByDataHoraAsc(solicitacaoId).stream()
				.map(AuditoriaSolicitacaoResponse::from)
				.toList();
	}

	private ContextoMensagemVoz contextoDaMensagem(SolicitacaoEmbarque solicitacao) {
		Passageiro passageiro = solicitacao.getPassageiro();
		Linha linha = solicitacao.getLinha();
		return new ContextoMensagemVoz(
				passageiro != null ? passageiro.getTipoNecessidade() : null,
				linha != null ? linha.getLetreiro() : null,
				linha != null ? linha.getDestino() : null,
				solicitacao.getTotem().getParada().getNome(),
				solicitacao.getTipoSinal());
	}

	public SolicitacaoEmbarque buscarEntidade(Long id) {
		return solicitacaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao de embarque", id));
	}

}
