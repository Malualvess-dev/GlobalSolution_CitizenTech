package br.com.fiap.citizentech.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.fiap.citizentech.config.RabbitConfig;
import br.com.fiap.citizentech.service.AvisoMotoristaService;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "citizentech.mensageria.modo", havingValue = "rabbit", matchIfMissing = true)
@RequiredArgsConstructor
public class AvisoMotoristaListener {

	private static final Logger log = LoggerFactory.getLogger(AvisoMotoristaListener.class);

	private final AvisoMotoristaService avisoMotoristaService;

	@RabbitListener(queues = RabbitConfig.FILA_SOLICITACAO_CRIADA)
	public void receber(SolicitacaoCriadaEvent evento) {
		log.info("Evento solicitacao.criada recebido para a solicitacao {}", evento.solicitacaoId());
		avisoMotoristaService.avisar(evento.solicitacaoId());
	}

}
