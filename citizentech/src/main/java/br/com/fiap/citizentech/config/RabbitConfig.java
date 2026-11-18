package br.com.fiap.citizentech.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_SOLICITACOES = "citizentech.solicitacoes";
	public static final String ROUTING_KEY_SOLICITACAO_CRIADA = "solicitacao.criada";
	public static final String FILA_SOLICITACAO_CRIADA = "citizentech.solicitacao.criada";

	static final String EXCHANGE_DLX = "citizentech.solicitacoes.dlx";
	static final String FILA_SOLICITACAO_CRIADA_DLQ = "citizentech.solicitacao.criada.dlq";

	@Bean
	public DirectExchange solicitacoesExchange() {
		return new DirectExchange(EXCHANGE_SOLICITACOES);
	}

	@Bean
	public Queue filaSolicitacaoCriada() {
		return QueueBuilder.durable(FILA_SOLICITACAO_CRIADA)
				.deadLetterExchange(EXCHANGE_DLX)
				.deadLetterRoutingKey(ROUTING_KEY_SOLICITACAO_CRIADA)
				.build();
	}

	@Bean
	public Binding bindingSolicitacaoCriada() {
		return BindingBuilder.bind(filaSolicitacaoCriada())
				.to(solicitacoesExchange())
				.with(ROUTING_KEY_SOLICITACAO_CRIADA);
	}

	@Bean
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(EXCHANGE_DLX);
	}

	@Bean
	public Queue filaSolicitacaoCriadaDlq() {
		return QueueBuilder.durable(FILA_SOLICITACAO_CRIADA_DLQ).build();
	}

	@Bean
	public Binding bindingSolicitacaoCriadaDlq() {
		return BindingBuilder.bind(filaSolicitacaoCriadaDlq())
				.to(deadLetterExchange())
				.with(ROUTING_KEY_SOLICITACAO_CRIADA);
	}

	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter("br.com.fiap.citizentech.messaging");
	}

}
