package br.com.fiap.citizentech.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

@Service
public class MensagemVozService {

	private static final Logger log = LoggerFactory.getLogger(MensagemVozService.class);

	static final int TAMANHO_MAXIMO = 300;

	private static final String INSTRUCOES = """
			Voce escreve as frases faladas por um totem de parada de onibus em Sao Paulo.
			O totem ajuda passageiros com deficiencia ou mobilidade reduzida a embarcar:
			quando o passageiro aciona o totem, o motorista do proximo onibus e avisado.

			Regras da frase:
			- Portugues do Brasil, tom acolhedor e direto, no maximo 2 frases e 250 caracteres.
			- Texto puro para leitura em voz alta: sem emojis, listas, aspas, siglas ou numeros de telefone.
			- Confirme que o pedido de embarque foi registrado.
			- Se a linha e o destino forem informados, cite os dois.
			- Nao prometa horario de chegada nem diga quantos minutos faltam.
			- Adapte a orientacao a necessidade:
			  BAIXA_VISAO: peca para ficar perto do totem; o onibus sera anunciado por voz.
			  CADEIRA_DE_RODAS: diga que o motorista vai preparar a rampa de acesso.
			  MOBILIDADE_REDUZIDA ou IDOSO: diga que o motorista vai esperar o embarque com calma.
			  NAO_INFORMADA: use uma orientacao geral para aguardar na parada.
			Responda somente com a frase.
			""";

	private static final String PEDIDO = """
			Necessidade do passageiro: {necessidade}
			Linha: {linha}
			Destino: {destino}
			Parada: {parada}
			Acionamento: {sinal}
			""";

	private final ChatClient chatClient;

	public MensagemVozService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.defaultSystem(INSTRUCOES).build();
	}

	public String gerar(ContextoMensagemVoz contexto) {
		try {
			String resposta = chatClient.prompt()
					.user(usuario -> usuario.text(PEDIDO)
							.param("necessidade", valor(contexto.tipoNecessidade(), "NAO_INFORMADA"))
							.param("linha", valor(contexto.letreiroLinha(), "nao informada"))
							.param("destino", valor(contexto.destinoLinha(), "nao informado"))
							.param("parada", valor(contexto.nomeParada(), "nao informada"))
							.param("sinal", contexto.tipoSinal() == TipoSinal.CARTAO
									? "cartao do passageiro" : "botao do totem"))
					.call()
					.content();

			String mensagem = limpar(resposta);
			if (!mensagem.isEmpty()) {
				return mensagem;
			}
			log.warn("A IA devolveu uma mensagem vazia; usando a mensagem padrao");
		} catch (RuntimeException e) {
			log.warn("Falha ao gerar a mensagem de voz com IA; usando a mensagem padrao: {}", e.getMessage());
		}
		return mensagemPadrao(contexto);
	}

	String mensagemPadrao(ContextoMensagemVoz contexto) {
		StringBuilder frase = new StringBuilder("Pedido de embarque registrado");
		if (contexto.letreiroLinha() != null) {
			frase.append(" para a linha ").append(contexto.letreiroLinha());
			if (contexto.destinoLinha() != null) {
				frase.append(", sentido ").append(contexto.destinoLinha());
			}
		}
		frase.append(". ").append(orientacao(contexto.tipoNecessidade()));
		return frase.toString();
	}

	private String orientacao(TipoNecessidade necessidade) {
		if (necessidade == null) {
			return "Aguarde na parada, o motorista do proximo onibus sera avisado.";
		}
		return switch (necessidade) {
			case BAIXA_VISAO -> "Fique perto do totem, o onibus sera anunciado por voz quando chegar.";
			case CADEIRA_DE_RODAS -> "Aguarde na parada, o motorista vai preparar a rampa de acesso.";
			case MOBILIDADE_REDUZIDA, IDOSO -> "Aguarde na parada, o motorista vai esperar o seu embarque com calma.";
		};
	}

	static String limpar(String resposta) {
		if (resposta == null) {
			return "";
		}
		String texto = resposta.replaceAll("[\"“”]", "").replaceAll("\\s+", " ").trim();
		if (texto.length() <= TAMANHO_MAXIMO) {
			return texto;
		}
		String cortado = texto.substring(0, TAMANHO_MAXIMO);
		int ultimoEspaco = cortado.lastIndexOf(' ');
		return (ultimoEspaco > 0 ? cortado.substring(0, ultimoEspaco) : cortado).trim();
	}

	private static String valor(Object valor, String padrao) {
		return valor != null ? valor.toString() : padrao;
	}

}
