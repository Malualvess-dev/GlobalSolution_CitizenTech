package br.com.fiap.citizentech.ai;

import br.com.fiap.citizentech.model.enums.TipoNecessidade;
import br.com.fiap.citizentech.model.enums.TipoSinal;

public record ContextoMensagemVoz(
		TipoNecessidade tipoNecessidade,
		String letreiroLinha,
		String destinoLinha,
		String nomeParada,
		TipoSinal tipoSinal) {
}
