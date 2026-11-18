package br.com.fiap.citizentech.messaging;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.enums.TipoSinal;

public record SolicitacaoCriadaEvent(
		Long solicitacaoId,
		Long totemId,
		Long linhaId,
		TipoSinal tipoSinal,
		LocalDateTime criadaEm) {
}
