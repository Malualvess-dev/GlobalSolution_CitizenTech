package br.com.fiap.citizentech.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum StatusSolicitacao {
	CRIADA,
	MOTORISTA_AVISADO,
	EMBARCADO,
	NAO_ATENDIDA,
	SEM_ONIBUS,
	CANCELADA;

	public static final Set<StatusSolicitacao> ATIVOS = EnumSet.of(CRIADA, MOTORISTA_AVISADO, NAO_ATENDIDA);

	public boolean isFinal() {
		return !ATIVOS.contains(this);
	}

}
