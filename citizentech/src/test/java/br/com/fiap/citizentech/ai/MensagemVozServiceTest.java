package br.com.fiap.citizentech.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

class MensagemVozServiceTest {

	private static final ContextoMensagemVoz BAIXA_VISAO_COM_LINHA = new ContextoMensagemVoz(
			TipoNecessidade.BAIXA_VISAO, "6091-10", "Terminal Varginha", "Parada UBS", TipoSinal.CARTAO);

	private ChatClient chatClient;
	private MensagemVozService service;

	@BeforeEach
	void configurar() {
		chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
		ChatClient.Builder builder = mock(ChatClient.Builder.class);
		when(builder.defaultSystem(anyString())).thenReturn(builder);
		when(builder.build()).thenReturn(chatClient);
		service = new MensagemVozService(builder);
	}

	@Test
	void usaATextoGeradoPelaIa() {
		respostaDaIa("  \"Pedido registrado para a linha 6091-10, sentido Terminal Varginha.\n Fique perto do totem.\"  ");

		String mensagem = service.gerar(BAIXA_VISAO_COM_LINHA);

		assertThat(mensagem).isEqualTo("Pedido registrado para a linha 6091-10, sentido Terminal Varginha. Fique perto do totem.");
	}

	@Test
	void enviaAoModeloSoONecessarioSemDadosPessoais() {
		respostaDaIa("Pedido registrado.");

		service.gerar(BAIXA_VISAO_COM_LINHA);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Consumer<PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);

		verify(chatClient.prompt(), atLeastOnce()).user(captor.capture());
		PromptUserSpec pedido = mock(PromptUserSpec.class, RETURNS_SELF);
		captor.getAllValues().getLast().accept(pedido);

		verify(pedido).param("necessidade", "BAIXA_VISAO");
		verify(pedido).param("linha", "6091-10");
		verify(pedido).param("destino", "Terminal Varginha");
		verify(pedido).param("parada", "Parada UBS");
		verify(pedido).param("sinal", "cartao do passageiro");
	}

	@Test
	void cortaRespostaLongaNoLimiteDaColuna() {
		respostaDaIa("palavra ".repeat(80));

		String mensagem = service.gerar(BAIXA_VISAO_COM_LINHA);

		assertThat(mensagem).hasSizeLessThanOrEqualTo(MensagemVozService.TAMANHO_MAXIMO).endsWith("palavra");
	}

	@Test
	void usaFrasePadraoQuandoAIaFalha() {
		when(chatClient.prompt().user(any(Consumer.class)).call().content())
				.thenThrow(new RuntimeException("OpenAI fora do ar"));

		String mensagem = service.gerar(new ContextoMensagemVoz(
				TipoNecessidade.CADEIRA_DE_RODAS, "6091-10", "Terminal Varginha", "Parada UBS", TipoSinal.CARTAO));

		assertThat(mensagem).isEqualTo("Pedido de embarque registrado para a linha 6091-10, sentido Terminal Varginha. "
				+ "Aguarde na parada, o motorista vai preparar a rampa de acesso.");
	}

	@Test
	void usaFrasePadraoQuandoAIaDevolveVazio() {
		respostaDaIa("   ");

		String mensagem = service.gerar(new ContextoMensagemVoz(null, null, null, "Parada UBS", TipoSinal.BOTAO));

		assertThat(mensagem).isEqualTo("Pedido de embarque registrado. "
				+ "Aguarde na parada, o motorista do proximo onibus sera avisado.");
	}

	@SuppressWarnings("unchecked")
	private void respostaDaIa(String texto) {
		when(chatClient.prompt().user(any(Consumer.class)).call().content()).thenReturn(texto);
	}

}
