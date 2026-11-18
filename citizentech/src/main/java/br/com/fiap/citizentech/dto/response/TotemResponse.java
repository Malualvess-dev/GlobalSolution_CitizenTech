package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.Totem;
import br.com.fiap.citizentech.model.enums.StatusTotem;

public record TotemResponse(
		Long id,
		Long paradaId,
		String paradaNome,
		String identificador,
		StatusTotem status,
		LocalDateTime ultimoSinalEm,
		LocalDateTime criadoEm) {

	public static TotemResponse from(Totem totem) {
		return new TotemResponse(
				totem.getId(),
				totem.getParada().getId(),
				totem.getParada().getNome(),
				totem.getIdentificador(),
				totem.getStatus(),
				totem.getUltimoSinalEm(),
				totem.getCriadoEm());
	}

}
