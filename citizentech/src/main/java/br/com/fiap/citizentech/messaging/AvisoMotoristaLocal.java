package br.com.fiap.citizentech.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.fiap.citizentech.service.AvisoMotoristaService;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "citizentech.mensageria.modo", havingValue = "local")
@RequiredArgsConstructor
public class AvisoMotoristaLocal {

	private static final Logger log = LoggerFactory.getLogger(AvisoMotoristaLocal.class);

	private final AvisoMotoristaService avisoMotoristaService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	// Depois do commit a transacao original ja terminou
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void avisar(SolicitacaoCriadaEvent evento) {
		log.info("Modo local (sem RabbitMQ): avisando motoristas da solicitacao {}", evento.solicitacaoId());
		avisoMotoristaService.avisar(evento.solicitacaoId());
	}

}
