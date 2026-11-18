package br.com.fiap.citizentech.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.citizentech.model.Linha;
import br.com.fiap.citizentech.model.Parada;
import br.com.fiap.citizentech.model.SolicitacaoEmbarque;
import br.com.fiap.citizentech.model.TentativaEmbarque;
import br.com.fiap.citizentech.model.Totem;
import br.com.fiap.citizentech.model.enums.ResultadoTentativa;
import br.com.fiap.citizentech.model.enums.StatusSolicitacao;
import br.com.fiap.citizentech.model.enums.TipoSinal;
import br.com.fiap.citizentech.repository.LinhaRepository;
import br.com.fiap.citizentech.repository.ParadaRepository;
import br.com.fiap.citizentech.repository.SolicitacaoEmbarqueRepository;
import br.com.fiap.citizentech.repository.TentativaEmbarqueRepository;
import br.com.fiap.citizentech.repository.TotemRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AvisoMotoristaServiceTest {

	@Autowired
	private AvisoMotoristaService avisoMotoristaService;
	@Autowired
	private ParadaRepository paradaRepository;
	@Autowired
	private LinhaRepository linhaRepository;
	@Autowired
	private TotemRepository totemRepository;
	@Autowired
	private SolicitacaoEmbarqueRepository solicitacaoRepository;
	@Autowired
	private TentativaEmbarqueRepository tentativaRepository;

	private Totem totem;
	private Linha linha;

	@BeforeEach
	void prepararDados() {
		Parada parada = paradaRepository.save(Parada.builder().codigoSptrans(9001L).nome("Parada Teste").build());
		linha = linhaRepository.save(Linha.builder()
				.codigoSptrans(9002L).letreiro("6091-10").sentido(1).destino("Terminal Varginha").build());
		totem = totemRepository.save(Totem.builder()
				.parada(parada).identificador("TOTEM-9001").tokenHash("a".repeat(64)).build());
	}

	@Test
	void registraTentativaEMudaStatusParaMotoristaAvisado() {
		SolicitacaoEmbarque solicitacao = criarSolicitacao(linha, "chave-aviso-1");

		avisoMotoristaService.avisar(solicitacao.getId());

		assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.MOTORISTA_AVISADO);
		List<TentativaEmbarque> tentativas = tentativaRepository.findBySolicitacaoIdOrderByAlertadoEmAsc(solicitacao.getId());
		assertThat(tentativas).hasSize(1);
		assertThat(tentativas.getFirst().getResultado()).isEqualTo(ResultadoTentativa.PENDENTE);
		assertThat(tentativas.getFirst().getLinha()).isEqualTo(linha);
		assertThat(tentativas.getFirst().getVeiculo()).as("alerta para a linha toda (RNF10)").isNull();
	}

	@Test
	void eventoRepetidoNaoDuplicaATentativa() {
		SolicitacaoEmbarque solicitacao = criarSolicitacao(linha, "chave-aviso-2");

		avisoMotoristaService.avisar(solicitacao.getId());
		avisoMotoristaService.avisar(solicitacao.getId());

		assertThat(tentativaRepository.findBySolicitacaoIdOrderByAlertadoEmAsc(solicitacao.getId())).hasSize(1);
	}

	@Test
	void solicitacaoSemLinhaContinuaCriada() {
		SolicitacaoEmbarque solicitacao = criarSolicitacao(null, "chave-aviso-3");

		avisoMotoristaService.avisar(solicitacao.getId());

		assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CRIADA);
		assertThat(tentativaRepository.findBySolicitacaoIdOrderByAlertadoEmAsc(solicitacao.getId())).isEmpty();
	}

	@Test
	void solicitacaoQueNaoExisteEIgnorada() {
		avisoMotoristaService.avisar(999_999L);
	}

	private SolicitacaoEmbarque criarSolicitacao(Linha linhaDaSolicitacao, String chave) {
		return solicitacaoRepository.save(SolicitacaoEmbarque.builder()
				.totem(totem)
				.linha(linhaDaSolicitacao)
				.tipoSinal(TipoSinal.BOTAO)
				.chaveIdempotencia(chave)
				.build());
	}

}
