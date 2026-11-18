package br.com.fiap.citizentech.dto.response;

import java.time.LocalDateTime;

import br.com.fiap.citizentech.model.Cartao;

public record CartaoResponse(
		Long id,
		Long passageiroId,
		String passageiroNome,
		Boolean ativo,
		LocalDateTime criadoEm) {

	public static CartaoResponse from(Cartao cartao) {
		return new CartaoResponse(
				cartao.getId(),
				cartao.getPassageiro().getId(),
				cartao.getPassageiro().getNome(),
				cartao.getAtivo(),
				cartao.getCriadoEm());
	}

}
